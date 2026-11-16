package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTKnRFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.c.CPointerType;
import org.eclipse.cdt.internal.core.dom.parser.c.CStructure;
import org.eclipse.cdt.internal.core.dom.parser.c.CTypedef;

/**
 * Generates a sequence of "extern ..." declarations allowing 
 * to use externally all the (nonlocal) variables that 
 * are used in a C function and that are clearly defined
 * outside the {@link IASTTranslationUnit} where the function
 * is declared. Once it has been used, it cannot be reused 
 * for a different function.
 * 
 * @author simone
 */
public class CASTVisitorExternalReferences extends ASTVisitor {
	private final String functionName;
	private final HashSet<String> localVariables;
	private final HashSet<String> usedVariables;

	//used during the visit
	private boolean visitingTheFunction;
	private IScope scope;

	//the outputs

	/**
	 * The {@link IASTFunctionDefinition} for the
	 * C function with name {@code this.}{@link #functionName}.
	 * If the visited {@link IASTTranslationUnit} does not 
	 * have any definition for a function with name 
	 * {@code this.}{@link #functionName},
	 * it is set to {@code null}.
	 */
	private IASTFunctionDefinition functionDefinition;

	/**
	 * Stores all the names of the variables used 
	 * in the C function with name {@code this.}{@link #functionName}, 
	 * that are clearly defined outside the visited 
	 * {@link IASTTranslationUnit}, that contains
	 * the function. The names are mapped to their
	 * corresponding {@link IType}.
	 */
	private Map<String, IType> externalReferences;

	/**
	 * Stores the C code fragment that declares
	 * as extern all the variables in {@link #externalReferences}.
	 */
	private String externalDeclarations;

	public CASTVisitorExternalReferences(String pFunctionName) {
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;

		this.functionName = pFunctionName;
		this.localVariables = new HashSet<>();
		this.usedVariables = new HashSet<>();

		this.visitingTheFunction = false;
		this.scope = null;

		this.functionDefinition = null;
		this.externalReferences = null;
		this.externalDeclarations = null;
	}

	public void calculate(IASTTranslationUnit translationUnit) {
		translationUnit.accept(this);
	}

	/**
	 * Checks whether a declaration in the AST is 
	 * the definition of the C function we are looking for.
	 * 
	 * @param pDeclaration a {@link IASTDeclaration}.
	 * @return {@code true} iff {@code pDeclaration} 
	 *         is a {@link CASTFunctionDefinition}
	 *         whose declarator has {@code this.functionName}
	 *         as name.
	 */
	private boolean isTheRightFunction(IASTDeclaration pDeclaration) {
		if (pDeclaration instanceof CASTFunctionDefinition) {
			CASTFunctionDefinition function = (CASTFunctionDefinition) pDeclaration;
			IASTFunctionDeclarator functionDeclarator = function.getDeclarator();
			if (functionDeclarator instanceof CASTFunctionDeclarator ||
					functionDeclarator instanceof CASTKnRFunctionDeclarator) {
				// It is a C function. Has it the right name?
				// Note that checking just the name of the function 
				// is enough since C does not allow overloading
				// of function names.
				IASTName functionName = functionDeclarator.getName();
				return functionName.getRawSignature().equals(this.functionName);
			} else {
				// It is a C++ function. This is quite 
				// unexpected: being pDeclaration 
				// a C (not a C++) function definition,
				// how can it be that its declarator
				// is that of a C++ function?
				// Well, apparently CDT does not 
				// disallow this situation, so we check
				// for it anyways (but we raise an exception
				// if it happens).
				throw new RuntimeException("Unable to instrument this type of function: " + functionDeclarator.getClass().toString());
			}
		}

		return false; // Not a C function definition
	}

	@Override
	public int leave(IASTTranslationUnit pTranslationUnit) {
		//nothing else to do, visit is over
		return PROCESS_ABORT;
	}

	@Override
	public int visit(IASTDeclaration pDeclaration) {
		if (isTheRightFunction(pDeclaration)) {
			// Got it! Visits it
			this.visitingTheFunction = true; // Signals start of visit
			this.functionDefinition = (IASTFunctionDefinition) pDeclaration;
			this.scope = ((IASTFunctionDefinition) pDeclaration).getScope();
		} else if (pDeclaration instanceof IASTFunctionDefinition) {
			// It is a function definition but not the one
			// we are looking for: skip it
			return PROCESS_SKIP;
		}

		return super.visit(pDeclaration);
	}

	@Override
	public int leave(IASTDeclaration pDeclaration) {
		// Signals end of visit (has no effect
		// if we are leaving the wrong function)
		this.visitingTheFunction = false;

		if (isTheRightFunction(pDeclaration)) {
			//nothing else to do, visit is over
			return PROCESS_ABORT;
		} else {
			return super.leave(pDeclaration);
		}
	}

	@Override
	public int visit(IASTName name) {
		if (this.visitingTheFunction) {
			if (name.isDeclaration()) {
				this.localVariables.add(String.valueOf(name.getLookupKey()));
			} else {
				this.usedVariables.add(String.valueOf(name.getLookupKey()));
			}
		}
		return super.visit(name);
	}

	// Post-visit results getters

	/**
	 * Returns the definition of the function.
	 * 
	 * @return a {@link IASTFunctionDefinition}.
	 */
	public IASTFunctionDefinition getFunctionDefinition() {
		return this.functionDefinition;
	}

	/**
	 * Returns the external references map.
	 * 
	 * @return a {@link Map}{@code <}{@link String}{@code , }{@link IType}{@code >},
	 *         whose keys are all the externally (i.e., not locally) declared 
	 *         variable names, and the mapped value is the declaration type 
	 *         of the corresponding variable. 
	 */
	public Map<String, IType> getExternalReferences() {
		if (this.externalReferences == null) {
			// Calculates the external variables as the set
			// of the variables used by the function, 
			// minus the locally declared ones
			List<String> externalVariables = new ArrayList<>(this.usedVariables);
			externalVariables.removeAll(this.localVariables);

			// Builds and caches the external references map
			Map<String, IType> externalReferences_ = new HashMap<>();
			for (String externalVariable : externalVariables) {
				IBinding[] bindings = this.scope.find(externalVariable);
				if (bindings.length > 0 && bindings[0] instanceof IVariable) {
					IVariable variable = (IVariable) bindings[0];
					externalReferences_.put(externalVariable, variable.getType());
				} //else, it could be another external symbol (e.g., a function)
			}
			this.externalReferences = externalReferences_;
		}
		return this.externalReferences;
	}

	/**
	 * Unwinds a C typedef by recursively 
	 * traversing the type alias relation.
	 * 
	 * @param type an {@link IType}.
	 * @return {@code type} if it is not a typedef,
	 *         otherwise the non-typedef {@link IType} 
	 *         that {@code type} recursively aliases.
	 */
	private static IType unwindTypedef(IType type) {
		IType result = type;
		while (result instanceof CTypedef) {
			result = ((CTypedef) result).getType();
		}
		return result;
	}

	/**
	 * Attempts to produce a normalized C language 
	 * textual declaration for a type. 
	 * 
	 * @param type an {@link IType}.
	 * @return {@code type} where all the nested
	 *         typedefs have been unwound, 
	 *         and finally converted to {@link String}.
	 */
	private static String getStringType(IType type) {
		//gets rid of the possible initial 
		//sequence of '*', finding the 
		//innermost pointed-to type.
		IType typeInner = type;
		int numberOfPointerSymbols = 0;
		while (typeInner instanceof CPointerType) {
			typeInner = ((CPointerType) typeInner).getType();
			numberOfPointerSymbols++;
		}

		//gets rid of typedefs
		//TODO First fix HERE.... to add the STRUCT keyword for the external.
		IType nestedType = unwindTypedef(type);
		return (nestedType instanceof CStructure ? "struct " : "") + nestedType.toString() + StringUtils.repeat('*', numberOfPointerSymbols);
	}

	/**
	 * Returns the "extern ..." declarations for all the
	 * variables that are not declared locally.
	 * 
	 * @return a {@link String} containing the "extern ..."
	 *         declarations for all the externally (i.e., not locally) 
	 *         declared variable names.
	 */
	public String getExternalDeclarations() {
		if (this.externalDeclarations == null) {
			Map<String, IType> types = getExternalReferences();
			StringBuilder externalDeclarations_ = new StringBuilder();
			for (Entry<String, IType> entry : types.entrySet()) {
				String name = entry.getKey();
				String type = getStringType(entry.getValue());
				externalDeclarations_.append("extern ");
				externalDeclarations_.append(type);
				externalDeclarations_.append(' ');
				externalDeclarations_.append(name);
				externalDeclarations_.append(";\n");
			}		
			this.externalDeclarations = externalDeclarations_.toString();
		}
		return this.externalDeclarations;
	}
}