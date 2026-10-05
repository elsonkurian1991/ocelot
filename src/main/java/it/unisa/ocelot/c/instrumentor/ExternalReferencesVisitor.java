package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * Generates the "extern ..." declarations for all the variables that
 * are used in the C code, but are not declared locally.
 * Cannot do multiple visits (use it once).
 * 
 * @author simone
 */
public class ExternalReferencesVisitor extends ASTVisitor {
	private String functionName;
	private boolean visitingTheFunction;	
	private Set<String> localVariables;
	private Set<String> usedVariables;
	
	//the outputs
	private IASTFunctionDefinition functionDefinition;
	private Map<String, IType> externalReferences;
	private String externalDeclarations;
	
	//set during visit
	private IScope scope;
		
	public ExternalReferencesVisitor(String pFunctionName) {
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;
		
		this.functionName = pFunctionName;
		this.visitingTheFunction = false;
		this.localVariables = new HashSet<>();
		this.usedVariables = new HashSet<>();
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
	 *         is the definition of a C function
	 *         whose name is {@code this.functionName}.
	 */
	private boolean isTheRightFunction(IASTDeclaration pDeclaration) {
		if (pDeclaration instanceof CASTFunctionDefinition) {
			CASTFunctionDefinition function = (CASTFunctionDefinition) pDeclaration;
			IASTFunctionDeclarator functionDeclarator = function.getDeclarator();
			if (functionDeclarator instanceof CASTFunctionDeclarator ||
				functionDeclarator instanceof CASTKnRFunctionDeclarator) {
				// is it the function we are looking for?
				// Note that checking just the function name should 
				// be enough since in C there is no function overloading
				IASTName functionName = functionDeclarator.getName();
				return functionName.getRawSignature().equals(this.functionName);
			} else {
				// it is a C++ function declarator; this is
				// quite unexpected, since pDeclaration is 
				// a C (not a C++) function definition...
				// Well, Eclipse CDT apparently allows it
				throw new RuntimeException("Unable to instrument this type of function: " + functionDeclarator.getClass().toString());
			}
		}
		
		return false; // Not a C function definition
	}
	
	@Override
	public int visit(IASTDeclaration pDeclaration) {
		if (isTheRightFunction(pDeclaration)) {
			// Got it! Visits it
			this.visitingTheFunction = true; // Signals start of visit
			this.functionDefinition = (IASTFunctionDefinition) pDeclaration;
			this.scope = ((IASTFunctionDefinition) pDeclaration).getScope();
		} else if (pDeclaration instanceof IASTFunctionDefinition) {
			// It's a function but not the right one:
			// skip it
			return PROCESS_SKIP;
		}

		return super.visit(pDeclaration);
	}
	
	@Override
	public int leave(IASTDeclaration pDeclaration) {
		if (this.isTheRightFunction(pDeclaration)) {
			this.visitingTheFunction = false; // Signals end of visit
		}
		return super.leave(pDeclaration);
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
			// Calculates the external variables (all used variables 
			// minus the locally declared ones)
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
	 * Unwinds a chain of C typedefs.
	 * 
	 * @param type an {@link IType}.
	 * @return {@code type} if it is not a typedef,
	 *         otherwise (recursively) the defining
	 *         type of the typedef.
	 */
	private static IType getType(IType type) {
		IType retVal = type;
		while (retVal instanceof CTypedef) {
			retVal = ((CTypedef) retVal).getType();
		}
		return retVal;
	}
	
	/**
	 * Unwinds chains of C typedefs. Handles a little
	 * pointers and struct (only at the top level).
	 * 
	 * @param type an {@link IType}.
	 * @return {@code type} where all the nested
	 *         typedefs have been unwound, 
	 *         and finally converted to {@link String}.
	 */
	private static String getStringType(IType type) {
		//counts the number of *s (pointer decl)
		//and returns the "pointer to..." type
		int pointers = 0;
		while (type instanceof CPointerType) {
			type = ((CPointerType) type).getType();
			pointers++;
		}
		
		//gets rid of typedefs and converts to String
		IType nestedType = getType(type); // TODO First fix HERE.... to add the STRUCT keyword for the external.
		return (nestedType instanceof CStructure ? "struct " : "") + nestedType.toString() + StringUtils.repeat('*', pointers);
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
				String type = getStringType(entry.getValue());
				String name = entry.getKey();
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