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
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTKnRFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.c.CEnumeration;
import org.eclipse.cdt.internal.core.dom.parser.c.CPointerType;
import org.eclipse.cdt.internal.core.dom.parser.c.CQualifierType;
import org.eclipse.cdt.internal.core.dom.parser.c.CStructure;
import org.eclipse.cdt.internal.core.dom.parser.c.CTypedef;

/**
 * Generates the macro that will contain the call. Also, it removes all 
 * typedefs from the tree, putting them in the "typedef" field.
 * Cannot do multiple visits (use it once).
 * 
 * @author simone
 */
public class MacroDefinerVisitor extends ASTVisitor {
	private String functionName;
	private Map<String, IType> externalReferences;
	private ConfigManager config;
	private Map<String, IParameter> parameters; //maps parameters names to their syntactic objects
	
	//the output
	private List<IType> functionParameters;
	private String callMacro;
	
	//set during visit
	private String[] parametersNames;
	
	public MacroDefinerVisitor(String pFunctionName, Map<String, IType> externalReferences) {
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;
		
		this.functionName = pFunctionName;
		this.externalReferences = externalReferences;
		try {
			this.config = ConfigManager.getInstance();
		} catch (IOException e) {
			throw new RuntimeException("No config file found!");
		}
		this.parameters = new HashMap<>();
		this.functionParameters = new ArrayList<>();
		this.callMacro = "";
	}
	
	public void calculate(IASTTranslationUnit translationUnit) {
		translationUnit.accept(this);
	}
	
	/**
	 * Checks whether a node in the AST is 
	 * the definition of the C function we are looking for.
	 * 
	 * @param pNode a {@link IASTNode}.
	 * @return {@code true} iff {@code pNode} 
	 *         is the definition of a C function
	 *         whose name is {@code this.functionName}.
	 */
	private boolean isTheRightFunction(IASTNode pNode) {
		if (pNode instanceof CASTFunctionDefinition) {
			CASTFunctionDefinition function = (CASTFunctionDefinition) pNode;
			IASTFunctionDeclarator functionDeclarator = function.getDeclarator();
			if (functionDeclarator instanceof CASTFunctionDeclarator ||
				functionDeclarator instanceof CASTKnRFunctionDeclarator) {
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
		// If it is not in the function it's looking for, skips it
		if (!isTheRightFunction(pDeclaration)) {
			return PROCESS_CONTINUE;
		}

		IASTFunctionDeclarator functionDeclarator = ((CASTFunctionDefinition) pDeclaration).getDeclarator();

		// gets types and names of all the parameters (as Strings)
		String[] parametersTypes;
		String[] parametersNames;
		if (functionDeclarator instanceof CASTFunctionDeclarator) {
			CASTFunctionDeclarator declarator = (CASTFunctionDeclarator) functionDeclarator;
			IASTParameterDeclaration[] parameters = declarator.getParameters();
			int nParameters = parameters.length;

			// fills parametersTypes/parametersNames
			parametersTypes = new String[nParameters];
			parametersNames = new String[nParameters];
			for (int i = 0; i < nParameters; i++) {
				parametersTypes[i] = parameters[i].getDeclSpecifier().getRawSignature();
				parametersNames[i] = parameters[i].getDeclarator().getRawSignature().replaceAll("\\*\\s*", "");
			}
		} else if (functionDeclarator instanceof CASTKnRFunctionDeclarator) {
			CASTKnRFunctionDeclarator declarator = (CASTKnRFunctionDeclarator) functionDeclarator;
			IASTName[] parameters = declarator.getParameterNames();
			int nParameters = parameters.length;

			// fills parametersTypes/parametersNames
			parametersTypes = new String[nParameters];
			parametersNames = new String[nParameters];
			for (int i = 0; i < nParameters; i++) {
				IASTName paramName = parameters[i];
				IASTDeclarator paramDeclarator = declarator.getDeclaratorForParameterName(paramName);
				String paramType;
				if (paramDeclarator == null) {
					paramType = "int";
				} else {
					paramType = ((IASTSimpleDeclaration) paramDeclarator.getParent()).getDeclSpecifier().getRawSignature();
				}
				parametersTypes[i] = paramType;
				parametersNames[i] = paramName.getRawSignature().replaceAll("\\*\\s*", "");
			}
		} else {
			// it is a C++ function declarator; this is
			// quite unexpected, since pDeclaration is 
			// a C (not a C++) function definition...
			// Well, Eclipse CDT apparently allows it
			throw new RuntimeException("Unable to instrument this type of function: " + functionDeclarator.getClass().toString());
		}

		this.parametersNames = parametersNames; //TODO what about parametersTypes? Why do we calculate it?

		return super.visit(pDeclaration);
	}
	
	/**
	 * Unwinds a chain of typedefs/C qualifiers.
	 * 
	 * @param type an {@link IType}.
	 * @return {@code type} if it is not a typedef/C qualifier,
	 *         otherwise (recursively) the defining
	 *         type of the typedef/C qualifier.
	 */
	private IType getType(IType type) {
		IType retVal = type;
		while (retVal instanceof CTypedef || retVal instanceof CQualifierType) {
			if (retVal instanceof CTypedef) {
				retVal = ((CTypedef) retVal).getType();
			} else { // (retVal instanceof CQualifierType)
				retVal = ((CQualifierType) retVal).getType();
			}
		}
		return retVal;
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
		// If it is not in the function it's looking for, skips it
		if (!isTheRightFunction(pDeclaration)) {
			return PROCESS_CONTINUE;
		}

		int result = super.leave(pDeclaration);
		
		IASTFunctionDeclarator functionDeclarator = ((CASTFunctionDefinition) pDeclaration).getDeclarator();

		// gets types and names of all the parameters (as Strings)
		String[] parametersTypes;
		String[] parametersNames;
		if (functionDeclarator instanceof CASTFunctionDeclarator) {
			CASTFunctionDeclarator declarator = (CASTFunctionDeclarator) functionDeclarator;
			IASTParameterDeclaration[] parameters = declarator.getParameters();
			int nParameters = parameters.length;

			// fills parametersTypes/parametersNames
			parametersTypes = new String[nParameters];
			parametersNames = new String[nParameters];
			for (int i = 0; i < nParameters; i++) {
				parametersTypes[i] = parameters[i].getDeclSpecifier().getRawSignature();
				parametersNames[i] = parameters[i].getDeclarator().getRawSignature();
			}
		} else if (functionDeclarator instanceof CASTKnRFunctionDeclarator) {
			CASTKnRFunctionDeclarator declarator = (CASTKnRFunctionDeclarator) functionDeclarator;
			IASTName[] parameters = declarator.getParameterNames();
			int nParameters = parameters.length;

			// fills parametersTypes/parametersNames
			parametersTypes = new String[nParameters];
			parametersNames = new String[nParameters];
			for (int i = 0; i < nParameters; i++) {
				IASTName paramName = parameters[i];
				IASTDeclarator paramDeclarator = declarator.getDeclaratorForParameterName(paramName);
				String paramType;
				if (paramDeclarator == null) {
					paramType = "int";
				} else {
					paramType = ((IASTSimpleDeclaration) paramDeclarator.getParent()).getDeclSpecifier().getRawSignature();
				}

				parametersTypes[i] = paramType;
				parametersNames[i] = paramName.getRawSignature();
			}
		} else {
			// it is a C++ function declarator; this is
			// quite unexpected, since pDeclaration is 
			// a C (not a C++) function definition...
			// Well, Eclipse CDT apparently allows it
			throw new RuntimeException("Unable to instrument this type of function: " + functionDeclarator.getClass().toString());
		}

		//TODO what is this for?
		for (String parameterName : parametersNames) {
			if (parameterName.contains("*")) { //TODO when does this happen?
				//TODO need to handle this case
			}
		}

		//TODO what is this for?
		for (Entry<String, IType> type : this.externalReferences.entrySet()) {
			IType realType = getType(type.getValue());

			if (realType instanceof CPointerType) {
				//TODO need to handle this case
			}
		}

		// Starts building the macro
		int nParameters = parametersTypes.length;
		StringBuilder macro = new StringBuilder();
		macro.append("#define OCELOT_CORES ");
		macro.append(this.config.getThreads());
		macro.append('\n');
		macro.append("#define OCELOT_ARRAYS_SIZE ");
		macro.append(this.config.getTestArraysSize());
		macro.append('\n');
		macro.append("#include \"CBridge.h\"\n");
		macro.append("#define OCELOT_TYPES {");
		
		// Starts calculating OCELOT_TYPES into var ocelotTypes
		List<String> ocelotTypes = new ArrayList<>();
		for (int i = 0; i < nParameters; i++) {
			//the void parameter is the only parameter in
			//zero-args function declarations like: int f(void);
			if (parametersTypes[i].contentEquals("void")) {
				//add this part to handle void in the parameter
				continue;
			}
			
			String parameterName = parametersNames[i].replaceAll("\\*\\s*", "");
			IParameter parameter = this.parameters.get(parameterName);
			IType parameterType = getType(parameter.getType());

			if (parameterType instanceof CStructure) {
				//TODO handle C structures with pointer field (currently we don't know how yet.)
				
				VarStructTree tree = new VarStructTree("", (CStructure) parameterType);
				List<StructNode> basics = tree.getBasicVariables();				
				for (StructNode var : basics) {
					//						this.functionParameters.add(var.type);
					//						
					if (var.isPointer()) {
						System.err.println("We can't handle structures with pointers yet.");
						//							String argcall = "OCELOT_NUM(OCELOT_ARGUMENT_POINTER(" + pointerArgument + "))";
						//							macro += var.getCompleteName() + " = " + "&_v_ocelot_pointers[(int)(" + argcall + ")];\\\n";
						//							pointerArgument++;
						//							//TODO Test!
					}
				}
				//					callParameters[outputArgument] = StringUtils.repeat('&', pointers) + "__arg" + outputArgument;
			} else { //any other non-C-structure parameter
				//TODO what is this for? Perhaps pointers?
				//TODO note that parametersNames[i] is NOT equal to parameterName!
				if (parametersNames[i].contains("*")) {
					IType realType = parameterType;
					if (parameterType instanceof CPointerType) {
						realType = getType(((CPointerType) parameterType).getType());
					}

					if (realType instanceof CStructure) {
						//just don't do anything...
					} else {
						//builds an OCELOT_TYPE name and adds to
						String typeString = parameterType.toString().replaceAll("\\*", "");
						typeString = typeString.replaceAll("const", "");
						typeString = typeString.replaceAll(" ", "");
						typeString = typeString.toUpperCase();

						ocelotTypes.add("TYPE_" + typeString);
					}
				}
			}
		}

		// Adds more stuff to ocelotTypes (extern stuff)
		for (Entry<String, IType> type : this.externalReferences.entrySet()) {
			IType realType = getType(type.getValue());
			if (realType instanceof CPointerType) {
				String typeString = realType.toString().replaceAll("\\*", "");
				typeString = typeString.replaceAll("const", "");
				typeString = typeString.replaceAll(" ", "");
				typeString = typeString.toUpperCase();

				ocelotTypes.add("TYPE_" + typeString);
			}
		}

		macro.append(StringUtils.join(ocelotTypes, ','));
		macro.append("}\n");
		macro.append("#define EXECUTE_OCELOT_TEST ");
		
		// Calculates the arguments for the test function call...
		int outputArgument = 0;
		int inputArgument = 0;
		int pointerArgument = 0;
		String[] callArguments = new String[nParameters];
		
		// ...first, appends the parameters...
		for (int i = 0; i < nParameters; i++) {
			//the void parameter is the only parameter in
			//zero-args function declarations like: int f(void);
			if (parametersTypes[i].contentEquals("void")) {
				//add this part to handle void in the parameter.
				continue;
			}

			String parameterName = parametersNames[i].replaceAll("\\*\\s*", "");
			IParameter parameter = this.parameters.get(parameterName);	
			IType parameterType = getType(parameter.getType());
			IType parameterTypeReal;
			if (parameterType instanceof CPointerType) {
				parameterTypeReal = getType(((CPointerType) parameterType).getType());
			} else {
				parameterTypeReal = parameterType;
			}
			int pointers = parametersNames[i].length() - parametersNames[i].replaceAll("\\*", "").length();
			String argumentNameOutput = "__arg" + outputArgument;
			String argumentNameInput = "__str" + inputArgument;
			String ocelotArgumentValue = "(OCELOT_NUM(OCELOT_ARGUMENT_VALUE(" + inputArgument + ")))";
			String ocelotArgumentPointer = "(OCELOT_NUM(OCELOT_ARGUMENT_POINTER(" + pointerArgument + ")))";
			String ocelotArgumentPointerTable = "&_v_ocelot_pointers[(int)(" + ocelotArgumentPointer + ")]";

			if (parameterType instanceof CStructure) {
				//declares a struct variable
				macro.append(parameterType); //Type
				macro.append(' ');
				macro.append(argumentNameOutput);
				macro.append(";\\\n");
				
				//populates the struct variable's fields with data
				VarStructTree tree = new VarStructTree(argumentNameOutput, (CStructure) parameterType);
				List<StructNode> basics = tree.getBasicVariables();
				for (StructNode var : basics) {
					this.functionParameters.add(var.type);
					String fieldType = var.type.toString().replaceAll("\\*\\s*", "");
					//TODO CEnumeration??? See below
					if (var.isPointer()) {
						macro.append(var.getCompleteName());
						macro.append(" = ");
						macro.append(ocelotArgumentPointerTable);
						macro.append(";\\\n");
						pointerArgument++;
						//TODO Test!
					} else {
						macro.append(fieldType);
						macro.append(' ');
						macro.append(argumentNameInput);
						macro.append(" = (");
						macro.append(fieldType);
						macro.append(") ");
						macro.append(ocelotArgumentValue);
						macro.append(";\\\n"); //Assign
						macro.append(var.getCompleteName());
						macro.append(" = ");
						macro.append(argumentNameInput);
						macro.append(";\\\n");
						inputArgument++;
					}
				}
				
				//adds the struct variable as another argument for the call
				callArguments[outputArgument] = StringUtils.repeat('&', pointers) + argumentNameOutput;
			} else if (parameterTypeReal instanceof CStructure) {
				//declares a struct variable
				macro.append(parametersTypes[i].replace("const", "")); //Type
				macro.append(' ');
				macro.append(argumentNameOutput);
				macro.append(";\\\n");
				
				//populates the struct variable's fields with data
				VarStructTree tree = new VarStructTree(argumentNameOutput, (CStructure) parameterTypeReal);
				List<StructNode> basics = tree.getBasicVariables();
				for (StructNode var : basics) {
					this.functionParameters.add(var.type);
					String fieldType = var.type.toString().replaceAll("\\*\\s*", "");
					if (var.type instanceof CEnumeration) {
						macro.append("enum ");
						macro.append(fieldType);
						macro.append(' ');
						macro.append(argumentNameInput);
						macro.append(" = (enum ");
						macro.append(fieldType);
						macro.append(')');
						macro.append(ocelotArgumentValue);
						macro.append(";\\\n"); //Assign
						macro.append(var.getCompleteNameWithPointers());
						macro.append(" = ");
						macro.append(argumentNameInput);
						macro.append(";\\\n");
						inputArgument++;
					} else if (var.isPointer()) {
						macro.append(var.getCompleteNameWithPointers());
						macro.append(" = ");
						macro.append(ocelotArgumentPointerTable);
						macro.append(";\\\n");
						pointerArgument++;
						//TODO Test!
					} else { //neither an enumeration nor a pointer
						macro.append(fieldType);
						macro.append(' ');
						macro.append(argumentNameInput);
						macro.append(" = (");
						macro.append(fieldType);
						macro.append(')');
						macro.append(ocelotArgumentValue);
						macro.append(";\\\n"); //Assign
						macro.append(var.getCompleteNameWithPointers());
						macro.append(" = ");
						macro.append(argumentNameInput);
						macro.append(";\\\n");
						inputArgument++;
					}
				}
				
				//adds the struct variable as another argument for the call
				callArguments[outputArgument] = StringUtils.repeat('&', pointers) + argumentNameOutput;
			} else { //the parameter is not a C structure
				this.functionParameters.add(parameterType);
				if (pointers == 0) {
					macro.append(parameterType);//Type
					macro.append(' ');
					macro.append(argumentNameOutput); //Name
					macro.append(" = ");
					macro.append(ocelotArgumentValue);
					macro.append(";\\\n"); //Assign
					callArguments[outputArgument] = argumentNameOutput;
					inputArgument++;
				} else {
					//no macro text to append
					callArguments[outputArgument] = StringUtils.repeat('&', pointers - 1) + ocelotArgumentPointerTable;
					pointerArgument++;
				}
			}
			outputArgument++;
		}
	
		// ...then, appends the extern...
		//TODO nothing is added to call parameters!!! Is it ok?
		for (Entry<String, IType> entry : this.externalReferences.entrySet()) {
			IType type = entry.getValue();
			String variableName = entry.getKey();
			String argumentNameInput = "__str" + inputArgument;
			String ocelotArgumentValue = "(OCELOT_NUM(OCELOT_ARGUMENT_VALUE(" + inputArgument + ")))";
			String ocelotArgumentPointer = "(OCELOT_NUM(OCELOT_ARGUMENT_POINTER(" + pointerArgument + ")))";
			String ocelotArgumentPointerTable = "&_v_ocelot_pointers[(int)(" + ocelotArgumentPointer + ")]";
			IType realType = type;
			int pointers = 0;
			while (realType instanceof CPointerType) {
				realType = ((CPointerType) realType).getType();
				pointers++;
			}

			if (realType instanceof CStructure) {
				VarStructTree tree = new VarStructTree(variableName, (CStructure) type);
				List<StructNode> basics = tree.getBasicVariables();
				for (StructNode var : basics) {
					this.functionParameters.add(var.type);
					String fieldType = var.type.toString().replaceAll("\\*\\s*", "");
					if (var.isPointer()) {
						macro.append(var.getCompleteName()); 
						macro.append(" = ");
						macro.append(ocelotArgumentPointerTable);
						macro.append(";\\\n");
						pointerArgument++;
						//TODO Test!
					} else {
						macro.append(fieldType);
						macro.append(' ');
						macro.append(argumentNameInput);
						macro.append(" = ("); 
						macro.append(fieldType);
						macro.append(')');
						macro.append(ocelotArgumentValue);
						macro.append(";\\\n"); //Assign
						macro.append(var.getCompleteName());
						macro.append(" = ");
						macro.append(argumentNameInput); 
						macro.append(";\\\n");
						inputArgument++;
					}
				}
			} else {
				this.functionParameters.add(type);
				if (pointers == 0) {
					macro.append(variableName); 
					macro.append(" = ");
					macro.append(ocelotArgumentValue); 
					macro.append(";\\\n"); //Assign
					inputArgument++;
				} else {
					macro.append(variableName); 
					macro.append(" = ");
					macro.append(ocelotArgumentPointerTable);
					macro.append(";\\\n"); //Assign
					pointerArgument++;
				}
			}
		}

		// Finally, appends the function call
		macro.append("OCELOT_TESTFUNCTION (");
		macro.append(StringUtils.join(callArguments, ","));
		macro.append(");");

		this.callMacro = macro.toString();

		return result;
	}
	
	@Override
	public int visit(IASTStatement statement) {
		if (statement instanceof IASTCompoundStatement) {
			// If it is not in the function it's looking for, skips it
			if (!isTheRightFunction(statement.getParent())) {
				return PROCESS_CONTINUE;
			}
			
			// Fills this.parameters
			IScope scope = ((IASTCompoundStatement) statement).getScope();
			for (String parameterName : this.parametersNames) {
				IBinding[] bindings = scope.find(parameterName);
				if (bindings.length > 0 && bindings[0] instanceof IParameter) {
					IParameter parameter = (IParameter) bindings[0];
					this.parameters.put(parameterName, parameter);
				} else {
					System.err.println("An error is going to occur. I couldn't find the parameter in the scope, sorry.");
				}
			}
		}
		
		return super.visit(statement);
	}
	
	// Post-visit results getters
	
	public List<IType> getFunctionParameters() {
		return functionParameters;
	}
	
	public String getCallMacro() {
		return callMacro;
	}
}
