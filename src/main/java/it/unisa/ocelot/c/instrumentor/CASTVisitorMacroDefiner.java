package it.unisa.ocelot.c.instrumentor;

import static it.unisa.ocelot.c.instrumentor.Utils.getComponent;
import static it.unisa.ocelot.c.instrumentor.Utils.getName;
import static it.unisa.ocelot.c.instrumentor.Utils.isArray;
import static it.unisa.ocelot.c.instrumentor.Utils.isBool;
import static it.unisa.ocelot.c.instrumentor.Utils.isChar;
import static it.unisa.ocelot.c.instrumentor.Utils.isDouble;
import static it.unisa.ocelot.c.instrumentor.Utils.isEnum;
import static it.unisa.ocelot.c.instrumentor.Utils.isFloat;
import static it.unisa.ocelot.c.instrumentor.Utils.isInt;
import static it.unisa.ocelot.c.instrumentor.Utils.isLong;
import static it.unisa.ocelot.c.instrumentor.Utils.isLongDouble;
import static it.unisa.ocelot.c.instrumentor.Utils.isLongLong;
import static it.unisa.ocelot.c.instrumentor.Utils.isPointer;
import static it.unisa.ocelot.c.instrumentor.Utils.isSignedChar;
import static it.unisa.ocelot.c.instrumentor.Utils.isSignedInt;
import static it.unisa.ocelot.c.instrumentor.Utils.isSignedLong;
import static it.unisa.ocelot.c.instrumentor.Utils.isSignedLongLong;
import static it.unisa.ocelot.c.instrumentor.Utils.isSignedShort;
import static it.unisa.ocelot.c.instrumentor.Utils.isShort;
import static it.unisa.ocelot.c.instrumentor.Utils.isStruct;
import static it.unisa.ocelot.c.instrumentor.Utils.isUnion;
import static it.unisa.ocelot.c.instrumentor.Utils.isUnsignedChar;
import static it.unisa.ocelot.c.instrumentor.Utils.isUnsignedInt;
import static it.unisa.ocelot.c.instrumentor.Utils.isUnsignedLong;
import static it.unisa.ocelot.c.instrumentor.Utils.isUnsignedLongLong;
import static it.unisa.ocelot.c.instrumentor.Utils.isUnsignedShort;
import static it.unisa.ocelot.c.instrumentor.Utils.isVoid;
import static it.unisa.ocelot.c.instrumentor.Utils.strip;

import it.unisa.ocelot.conf.ConfigManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;

/**
 * Generates the macro that will contain the call. Also, it removes all 
 * typedefs from the tree, putting them in the "typedef" field.
 * Cannot do multiple visits (use it once).
 * 
 * @author simone
 */
public class CASTVisitorMacroDefiner extends ASTVisitor {
	private final IASTTranslationUnit translationUnit;
	private final String functionName;
	private final Map<String, IType> externalReferences;
	private final ConfigManager config;

	//the output
	private List<IType> parametersTypes;
	private StringBuilder macroConstants;
	private StringBuilder macroTypes;
	private StringBuilder macroExec;
	private StringBuilder callMacro;

	public CASTVisitorMacroDefiner(IASTTranslationUnit pTranslationUnit, String pFunctionName, Map<String, IType> pExternalReferences) {
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;
		this.translationUnit = pTranslationUnit;
		this.functionName = pFunctionName;
		this.externalReferences = pExternalReferences;
		try {
			this.config = ConfigManager.getInstance();
		} catch (IOException e) {
			throw new RuntimeException("No config file found!");
		}

		this.parametersTypes = null;
		this.macroConstants = null;
		this.macroTypes = null;
		this.macroExec = null;
		this.callMacro = null;
	}

	public void calculate() {
		this.translationUnit.accept(this);
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
		if (pNode instanceof IASTFunctionDefinition) {
			IASTFunctionDefinition functionDefinition = (IASTFunctionDefinition) pNode;
			IASTFunctionDeclarator functionDeclarator = functionDefinition.getDeclarator();
			IASTName functionName = functionDeclarator.getName();
			return functionName.getRawSignature().equals(this.functionName);
		}

		return false; // Not a function definition
	}

	@Override
	public int visit(IASTDeclaration pDeclaration) {
		// If it is not the function we're looking for, skips it
		if (!isTheRightFunction(pDeclaration)) {
			return PROCESS_CONTINUE;
		}

		// Gets types and names of all the parameters
		IASTFunctionDeclarator functionDeclarator = ((IASTFunctionDefinition) pDeclaration).getDeclarator();
		IASTName[] parametersASTNames;
		if (functionDeclarator instanceof IASTStandardFunctionDeclarator) {
			IASTStandardFunctionDeclarator functionASTDeclaratorStd = (IASTStandardFunctionDeclarator) functionDeclarator;
			IASTParameterDeclaration[] parametersASTDeclarations = functionASTDeclaratorStd.getParameters();
			IASTDeclarator[] parametersASTDeclarators = Arrays.stream(parametersASTDeclarations).map(IASTParameterDeclaration::getDeclarator).toArray(IASTDeclarator[]::new);
			parametersASTNames = Arrays.stream(parametersASTDeclarators).map(IASTDeclarator::getName).toArray(IASTName[]::new);
		} else { // (functionDeclarator instanceof ICASTKnRFunctionDeclarator)
			ICASTKnRFunctionDeclarator functionASTDeclaratorKnR = (ICASTKnRFunctionDeclarator) functionDeclarator;
			parametersASTNames = functionASTDeclaratorKnR.getParameterNames();
		}
		this.parametersTypes = Arrays.stream(parametersASTNames).map(IASTName::resolveBinding).map(b -> (IParameter) b).map(IParameter::getType).toList();

		return super.visit(pDeclaration);
	}

	private void buildMacros() {
		// Starts building the macros
		// 1- constants
		this.macroConstants = new StringBuilder();
		this.macroConstants.append("#define OCELOT_CORES ");
		this.macroConstants.append(this.config.getThreads());
		this.macroConstants.append('\n');
		this.macroConstants.append("#define OCELOT_ARRAY_LENGTH ");
		this.macroConstants.append(this.config.getTestArraysSize());
		this.macroConstants.append('\n');

		//useful for next steps; supplierAllSlotsBFS
		//generates streams of the breadth-first, left-to-right 
		//visit of all the slots forest with roots
		//the concatenation of parameters and externs, i,e:
		//parameters[0] ... externs[n], then
		//all the children (immediate subslots) of parameters[0], if any,
		//...
		//all the children (immediate subslots) of externs[n], if any,
		//all the children (immediate subslots) of parameters[0].children[0], if any,
		//...
		//all the children (immediate subslots) of parameters[0].children[k], if any,
		//...
		//all the children (immediate subslots) of externs[n].children[0], if any,
		//...
		//all the children (immediate subslots) of externs[n].children[k], if any,
		//...
		int nParameters;
		Supplier<Stream<Slot>> supplierAllSlotsStream;
		if (this.parametersTypes.size() == 1 && isVoid(this.parametersTypes.get(0))) {
			//a single void parameter happens with function declarations 
			//like: int f(void)
			nParameters = 0;
			supplierAllSlotsStream = () -> Stream.empty(); //no slots
		} else {
			nParameters = this.parametersTypes.size();
			supplierAllSlotsStream = () -> {
				Supplier<Stream<Slot>> parametersSlots = () -> this.parametersTypes.stream().map(Slot::make);
				Supplier<Stream<Slot>> externSlots = () -> this.externalReferences.entrySet().stream().map(e -> Slot.make(e.getValue(), e.getKey()));
				Supplier<Stream<Slot>> rootSlots = () -> Stream.concat(parametersSlots.get(), externSlots.get());
				return Stream.iterate(rootSlots, s -> s.get().spliterator().getExactSizeIfKnown() > 0, s -> (() -> s.get().flatMap(Slot::getChildren)))
				.map(Supplier::get).flatMap(Function.identity());
			};
		}
		
		// 2- types for arrays members
		this.macroTypes = new StringBuilder();
		this.macroTypes.append("#define OCELOT_TYPES { ");
		supplierAllSlotsStream.get().filter(Slot::isTypeArray)
		.forEach(slot -> { 
			addType(this.macroTypes, slot.getType());
			this.macroTypes.append(", ");
		});
		this.macroTypes.append("}\n");

		// 3- test function execution
		this.macroExec = new StringBuilder();
		this.macroExec.append("#define EXECUTE_OCELOT_TEST \\\n");

		// Makes an Iterable for all the slots
		Stream<Slot> allSlotsStream = supplierAllSlotsStream.get();
		Iterable<Slot> allSlots = (Iterable<Slot>) allSlotsStream::iterator;
		
		// Makes all the variable declarations and slot assignments
		// for all the parameters and externs
		int tmpVariableNumber = 0;
		int callArgumentNumber = 0;
		int ocelotValueNumber = 0;
		//int ocelotArrayNumber = 0; TODO not used!
		//int ocelotPointerNumber = 0; TODO not used!
		ArrayList<String> topVariablesNames = new ArrayList<>();
		for (Slot slot : allSlots) {
			// Creates and sets the variable name
			if (slot.hasNoVariableName()) {
				String variableName = (slot.isRoot() ? callArgument(callArgumentNumber++) : tmpVariable(tmpVariableNumber++));
				slot.setVariableName(variableName);
			}
			
			// Saves the variable name, if it is a
			// root slot, in the list of top
			// variable names
			if (slot.isRoot()) {
				topVariablesNames.add(slot.getVariableName());
			}
			
			// Declares the variable
			this.macroExec.append(slot.getTypeString());
			this.macroExec.append(' ');
			this.macroExec.append(slot.getVariableName());
			if (slot.isTypeArray()) {
				this.macroExec.append("[OCELOT_ARRAY_LENGTH]");
			}
			
			// If the variable is numeric, sets
			// its value
			if (slot.isTypeNumeric()) {
				this.macroExec.append(" = (");
				this.macroExec.append(slot.getTypeString());
				this.macroExec.append(") ");
				this.macroExec.append(value(ocelotValueNumber++));
			}
			this.macroExec.append(";\\\n");
			
			// If the slot has array type, creates 
			// and sets the variable names of all 
			// the array (sub)slots
			if (slot.isTypeArray()) {
				//note that slot has exactly one (syntactic) child
				Slot slotArray = slot.getChildren().findFirst().orElseThrow();
				for (int i = 0; i < this.config.getTestArraysSize(); ++i) {
					// Creates and sets the variable name
					String variableNameSlotArray = tmpVariable(tmpVariableNumber++);
					slotArray.addSibling(variableNameSlotArray);
				}
			}
			
			// If the slot descends from another slot,
			// sets the parent's slot via its accessor
			if (slot.isDescendant()) {
				this.macroExec.append(slot.getParentAccessor());
				this.macroExec.append(" = ");
				this.macroExec.append(slot.getValueForParentAccessor());
				this.macroExec.append(";\\\n");
			}
		}

		// Finally, appends the function call
		this.macroExec.append("OCELOT_TESTFUNCTION");
		this.macroExec.append(topVariablesNames.stream().limit(nParameters).collect(Collectors.joining(", ", "(", ")")));
	}

	private void addType(StringBuilder b, IType t) {
		IType ts = strip(t);
		if (isInt(ts)) {
			b.append("TYPE_INT");
		} else if (isSignedInt(ts)) {
			b.append("TYPE_SIGNED_INT");
		} else if (isUnsignedInt(ts)) {
			b.append("TYPE_UNSIGNED_INT");
		} else if (isShort(ts)) {
			b.append("TYPE_SHORT");
		} else if (isSignedShort(ts)) {
			b.append("TYPE_SIGNED_SHORT");
		} else if (isUnsignedShort(ts)) {
			b.append("TYPE_UNSIGNED_SHORT");
		} else if (isLong(ts)) {
			b.append("TYPE_LONG");
		} else if (isSignedLong(ts)) {
			b.append("TYPE_SIGNED_LONG");
		} else if (isUnsignedLong(ts)) {
			b.append("TYPE_UNSIGNED_LONG");
		} else if (isLongLong(ts)) {
			b.append("TYPE_LONGLONG");
		} else if (isSignedLongLong(ts)) {
			b.append("TYPE_SIGNED_LONGLONG");
		} else if (isUnsignedLongLong(ts)) {
			b.append("TYPE_UNSIGNED_LONGLONG");
		} else if (isChar(ts)) {
			b.append("TYPE_CHAR");
		} else if (isSignedChar(ts)) {
			b.append("TYPE_SIGNED_CHAR");
		} else if (isUnsignedChar(ts)) {
			b.append("TYPE_UNSIGNED_CHAR");
		} else if (isBool(ts)) {
			b.append("TYPE_BOOL");
		} else if (isFloat(ts)) {
			b.append("TYPE_FLOAT");
		} else if (isDouble(ts)) {
			b.append("TYPE_DOUBLE");
		} else if (isLongDouble(ts)) {
			b.append("TYPE_LONGDOUBLE");
		} else if (isVoid(ts)) {
			b.append("TYPE_VOID");
		} else if (isArray(ts)) {
			b.append("TYPE_ARRAYOF(");
			IType tComponent = getComponent(ts);
			addType(b, tComponent);
			b.append(')');
		} else if (isPointer(ts)) {
			b.append("TYPE_POINTERTO(");
			IType tComponent = getComponent(ts);
			addType(b, tComponent);
			b.append(')');
		} else if (isStruct(t)) {
			b.append("TYPE_STRUCT(");
			String name = getName(ts);
			b.append(name);
			b.append(')');
		} else if (isUnion(t)) {
			b.append("TYPE_UNION(");
			String name = getName(ts);
			b.append(name);
			b.append(')');
		} else if (isEnum(t)) {
			b.append("TYPE_ENUM(");
			String name = getName(ts);
			b.append(name);
			b.append(')');
		} else {
			throw new RuntimeException("Input slots of type " + t.toString() + " are not (yet) supported");
		}
	}
	
	private static String tmpVariable(int number) {
		return "__str" + number;
	}

	private static String callArgument(int number) {
		return "__arg" + number;
	}

	private static String value(int number) {
		return "(OCELOT_NUM(OCELOT_ARGUMENT_VALUE(" + number + ")))";
	}

	/*private static String array(int number) {
		return "(OCELOT_NUM(OCELOT_ARGUMENT_ARRAY(" + number + ")))";
	}

	private static String pointer(int number) {
		return "(OCELOT_NUM(OCELOT_ARGUMENT_POINTER(" + number + ")))";
	}
	
	private static String pointerTable(int number) {
		return "&_v_ocelot_pointers[(int)(" + pointer(number) + ")]";
	}*/


	// Post-visit results getters

	public List<IType> getFunctionParametersTypes() {
		return this.parametersTypes;
	}

	public String getCallMacro() {
		if (this.callMacro == null) {
			this.callMacro = new StringBuilder();
			if (this.macroConstants == null || this.macroTypes == null || this.macroExec == null) {
				buildMacros();
			}
			this.callMacro.append(this.macroConstants);
			this.callMacro.append(this.macroTypes);
			this.callMacro.append(this.macroExec);
		}
		
		return this.callMacro.toString();
	}
}
