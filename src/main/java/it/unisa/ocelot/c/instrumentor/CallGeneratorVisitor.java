package it.unisa.ocelot.c.instrumentor;

import it.unisa.ocelot.conf.ConfigManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;
import org.eclipse.cdt.core.model.IStructure;

/**
 * Generates the macro that will contain the call. Besides, it removes all typedefs from the tree, putting them in the
 * "typedef" field.
 * @author simone
 *
 */
public class CallGeneratorVisitor extends ASTVisitor {
	private String call;
	private String functionName;
	private String[] neededParameters;
	private List<IType> functionParameters;
	private Map<String, IParameter> parameters;
	
	private Map<String, IType> externalReferences;
	
	@SuppressWarnings("unused")
	private List<String> localVariables;
	@SuppressWarnings("unused")
	private List<String> usedVariables;
	
	public CallGeneratorVisitor(String pFunctionName, Map<String, IType> externalReferences) {
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;
		
		this.functionName = pFunctionName;
		
		this.externalReferences = externalReferences;
		
		this.functionParameters = new ArrayList<>();
		
		this.parameters = new HashMap<>();
		
		this.localVariables = new ArrayList<>();
		this.usedVariables = new ArrayList<>();
		
		this.call = "";
		
		try {
			ConfigManager.getInstance();
		} catch (IOException e) {
			throw new RuntimeException("No config file found!");
		}
	}
	
	public List<IType> getFunctionParameters() {
		return functionParameters;
	}
	
	private IType getType(IType type) {
		while (type instanceof ITypedef) {
			ITypedef tdef = (ITypedef) type;
			type = tdef.getType();
		}
		
		return type;
	}
	
	@Override
	public int visit(IASTDeclaration pDeclaration) {
		if (pDeclaration instanceof IASTFunctionDefinition) {
			IASTFunctionDefinition function = (IASTFunctionDefinition) pDeclaration;
			
			IASTName functionName;
			String[] parametersTypes;
			String[] parametersNames;
			if (function.getDeclarator() instanceof IASTStandardFunctionDeclarator) {
				IASTStandardFunctionDeclarator declarator = (IASTStandardFunctionDeclarator)function.getDeclarator();
				
				functionName = declarator.getName();
				parametersTypes = new String[declarator.getParameters().length];
				parametersNames = new String[declarator.getParameters().length];
				
				for (int i = 0; i < declarator.getParameters().length; i++) {
					parametersTypes[i] = declarator.getParameters()[i].getDeclSpecifier().getRawSignature();
					parametersNames[i] = declarator.getParameters()[i].getDeclarator().getRawSignature().replaceAll("\\*\\s*", "");
				}
				
			} else if (function.getDeclarator() instanceof ICASTKnRFunctionDeclarator) {
				ICASTKnRFunctionDeclarator declarator = (ICASTKnRFunctionDeclarator)function.getDeclarator();
				
				functionName = declarator.getName();
				parametersTypes = new String[declarator.getParameterNames().length];
				parametersNames = new String[declarator.getParameterNames().length];
				
				for (int i = 0; i < declarator.getParameterNames().length; i++) {
					IASTName paramName = declarator.getParameterNames()[i];
					IASTDeclarator declaration = declarator.getDeclaratorForParameterName(paramName);
					String type;
					if (declaration != null)
						type = ((IASTSimpleDeclaration)declaration.getParent()).getDeclSpecifier().getRawSignature();
					else
						type = "int";
					
					parametersNames[i] = paramName.getRawSignature().replaceAll("\\*\\s*", "");
					parametersTypes[i] = type;
				}
			} else {
				throw new RuntimeException("Unable to instrument this type of function: " + function.getDeclarator().getClass().toString());
			}
			
			//Goes on only if 
			if (!functionName.getRawSignature().equals(this.functionName))
				return PROCESS_CONTINUE;
			
			this.neededParameters = parametersNames;
		}
		
		return super.visit(pDeclaration);
	}
	
	/*
	 * NOTE:
	 * Assuming that all the structures are composed by either pointers or not pointers.
	 * If a field is a pointer, it will be assumed that this is of a C basic type.
	 * TODO Make sure that also in case of non basic C types this method works properly
	 * For example, Ocelot won't work if there is a structure with a field with type pointer
	 * to another struct, because it won't initialize the parameters of the pointed structure
	 */
	@Override
	public int leave(IASTDeclaration pDeclaration) {
		
		int result = super.leave(pDeclaration);
		if (pDeclaration instanceof IASTFunctionDefinition) {
			IASTFunctionDefinition function = (IASTFunctionDefinition)pDeclaration;
			
			IASTName functionName;
			String[] parametersStringTypes;
			String[] parametersStringNames;
			if (function.getDeclarator() instanceof IASTStandardFunctionDeclarator) {
				IASTStandardFunctionDeclarator declarator = (IASTStandardFunctionDeclarator)function.getDeclarator();
				
				functionName = declarator.getName();
				parametersStringTypes = new String[declarator.getParameters().length];
				parametersStringNames = new String[declarator.getParameters().length];
				
				for (int i = 0; i < declarator.getParameters().length; i++) {
					parametersStringTypes[i] = declarator.getParameters()[i].getDeclSpecifier().getRawSignature();
					parametersStringNames[i] = declarator.getParameters()[i].getDeclarator().getRawSignature();
				}
				
			} else if (function.getDeclarator() instanceof ICASTKnRFunctionDeclarator) {
				ICASTKnRFunctionDeclarator declarator = (ICASTKnRFunctionDeclarator)function.getDeclarator();
				
				functionName = declarator.getName();
				parametersStringTypes = new String[declarator.getParameterNames().length];
				parametersStringNames = new String[declarator.getParameterNames().length];
				
				for (int i = 0; i < declarator.getParameterNames().length; i++) {
					IASTName paramName = declarator.getParameterNames()[i];
					IASTDeclarator declaration = declarator.getDeclaratorForParameterName(paramName);
					String type;
					if (declaration != null)
						type = ((IASTSimpleDeclaration)declaration.getParent()).getDeclSpecifier().getRawSignature();
					else
						type = "int";
					
					parametersStringNames[i] = paramName.getRawSignature();
					parametersStringTypes[i] = type;
				}
			} else {
				throw new RuntimeException("Unable to instrument this type of function: " + function.getDeclarator().getClass().toString());
			}
			
			//Goes on only if we are on the right function
			if (!functionName.getRawSignature().equals(this.functionName))
				return PROCESS_CONTINUE;

//			int numberOfPointers = 0;
//			for (String stringName : parametersStringNames) {
//				if (stringName.contains("*"))
//					numberOfPointers++;
//			}
//			
//			for (Entry<String, IType> type : this.externalReferences.entrySet()) {
//				IType realType = getType(type.getValue());
//				
//				if (realType instanceof CPointerType)
//					numberOfPointers++;
//			}
			
//			int totalVariables = parametersStringTypes.length;
			
			String[] callParameters = new String[parametersStringTypes.length];
			String callString = "";
			
			int outputArgument = 0;
			int inputArgument = 0;
			int pointerArgument = 0;
			
			for (int i = 0; i < parametersStringTypes.length; i++) {
				String parameterStringName = parametersStringNames[i].replaceAll("\\*\\s*", "");
				String parameterStringType = parametersStringTypes[i];
				
				IParameter parameterRealType = this.parameters.get(parameterStringName);				
				IType type = getType(parameterRealType.getType());
				
				int pointers = parametersStringNames[i].length() - parametersStringNames[i].replaceAll("\\*", "").length();
				
				if (type instanceof ICompositeType) {
					callString += parameterStringType; //Type
					callString += " __arg" + outputArgument + ";\\\n";
					Slot tree = Slot.make(type, "__arg" + outputArgument);
					List<Slot> basics = tree.getLeaves();
					for (Slot basic : basics) {
						this.functionParameters.add(basic.type);
						String fieldType = basic.type.toString().replaceAll("\\*\\s*", "");
						
						if (basic.isTypePointer()) {
							String argcall = "__ptr" + pointerArgument;
							callString += basic.getParentAccessor() + " = " + argcall + ";\n";
							pointerArgument++;
							//TODO Test!
						} else {
							callString += fieldType;
							callString += " __str" + inputArgument;
							callString += " = (" + fieldType +")__val" + inputArgument + ";\n"; //Assign
							inputArgument++;
						}
					}
					callParameters[outputArgument] = StringUtils.repeat('&', pointers) + "__arg" + outputArgument;
				} else {
					this.functionParameters.add(type);
					
					if (pointers == 0) {
						callString += parameterStringType;//Type
						callString += " __arg" +outputArgument; //Name
						callString += " = __val" + inputArgument + ";\n"; //Assign
						
						callParameters[outputArgument] = "__arg" + outputArgument;
						inputArgument++;
					} else {
						String argcall = "__ptr" + pointerArgument;
						callParameters[outputArgument] = /*StringUtils.repeat('&', pointers) +*/ argcall;
						pointerArgument++;
					}
				}
				outputArgument++;
			}
			
			for (Entry<String, IType> entry : this.externalReferences.entrySet()) {
				IType type = entry.getValue();
				IType realType = type;
				String variableName = entry.getKey();
				
				int pointers = 0;
				
				while (realType instanceof IPointerType) {
					realType = ((IPointerType)realType).getType();
					pointers++;
				}
				
				if (realType instanceof IStructure) {
					Slot tree = Slot.make(type, variableName);
					List<Slot> basics = tree.getLeaves();
					for (Slot var : basics) {
						this.functionParameters.add(var.type);
						String fieldType = var.type.toString().replaceAll("\\*\\s*", "");
						
						if (var.isTypePointer()) {
							String argcall = "__ptr" + pointerArgument;
							callString += var.getParentAccessor() + " = " + argcall + ";\n";
							pointerArgument++;
							//TODO Test!
						} else {
							callString += fieldType;
							callString += " __str"+inputArgument;
							callString += " = (" + fieldType +")__val" + inputArgument + ";\n"; //Assign
							callString += var.getParentAccessor() + " = __str" + inputArgument + ";\\\n";
							inputArgument++;
						}
					}
				} else {
					this.functionParameters.add(type);
					
					if (pointers == 0) {
						callString += variableName + " = __val" + inputArgument + ";\n"; //Assign
						inputArgument++;
					} else {
						String argcall = "__ptr" + pointerArgument;
						callString += variableName + " = " + argcall + ";\n"; //Assign
						pointerArgument++;
					}
				}
			}
						
			callString += this.functionName + "(" + StringUtils.join(callParameters, ",") + ");\n";
			
			this.call = callString;
		}
		return result;
	}
	
	@Override
	public int visit(IASTStatement statement) {
		if (statement instanceof IASTCompoundStatement &&
				statement.getParent() instanceof IASTFunctionDefinition) {
			IASTFunctionDefinition function = (IASTFunctionDefinition)statement.getParent();
			if (function.getDeclarator().getName().getRawSignature().equals(this.functionName)) {
				for (String parameterName : this.neededParameters) {
					IBinding[] binds = ((IASTCompoundStatement) statement).getScope().find(parameterName);
					if (binds.length > 0) {
						this.parameters.put(parameterName, (IParameter)binds[0]);
					} else {
						System.err.println("An error is going to occur. I couldn't find the parameter in the scope, sorry.");
					}
				}
			}
		}
		
		return super.visit(statement);
	}
	
	@Override
	public int visit(IASTParameterDeclaration parameterDeclaration) {
		return super.visit(parameterDeclaration);
	}
	
	@Override
	public int leave(IASTTranslationUnit tu) {
		return super.leave(tu);
	}
	
	public String getCall() {
		return call;
	}
}
