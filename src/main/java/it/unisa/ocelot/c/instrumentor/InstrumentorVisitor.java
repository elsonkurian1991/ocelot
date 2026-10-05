package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.c.ICPointerType;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompositeTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTName;
import org.eclipse.cdt.internal.core.dom.parser.c.CTypedef;
import org.eclipse.cdt.internal.core.dom.rewrite.astwriter.ASTWriter;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGVisitor;
import it.unisa.ocelot.c.cfg.edges.CaseEdge;

public class InstrumentorVisitor extends ASTVisitor {
	private static final int op_invalid = -1;
	
	private String functionName;
	private Stack<List<IASTStatement>> switchExpressions;
	private List<IASTNode> typedefs;
	private List<IASTExpression> functionCallsInExpressions;
	
	public InstrumentorVisitor(String pInstrumentFunction) {
		this.shouldVisitExpressions = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitPointerOperators = true;
		
		this.functionName = pInstrumentFunction;
		this.switchExpressions = new Stack<>();
		this.typedefs = new ArrayList<>();
		this.functionCallsInExpressions = new ArrayList<>();
	}

	public void transform(IASTTranslationUnit translationUnit) {
		translationUnit.accept(this);
	}
	
	@Override
	public int visit(IASTTranslationUnit tu) {
		// Simulates the generation of a CFG in order to correctly retrieve the "case" unique ids.
		// The resulting CFG will never be used.
		// TODO define a lightweight visitor able to initialize the "case" unique ids only correctly.
		tu.accept(new CFGVisitor(new CFG(), this.functionName));
		return super.visit(tu);
	}
	
	@Override
	public int visit(IASTDeclaration name) {
		if (name instanceof CASTFunctionDefinition) {
			IASTFunctionDefinition function = (CASTFunctionDefinition)name;
			
			if (!function.getDeclarator().getName().getRawSignature().equals(this.functionName)) {
				return PROCESS_SKIP;
			}
		} else if (name instanceof IASTSimpleDeclaration) {
			if (name.getRawSignature().startsWith("typedef ")) {
				this.typedefs.add(name);
			}
			if (((IASTSimpleDeclaration) name).getDeclSpecifier() instanceof CASTCompositeTypeSpecifier) {
				name.setParent(null);
			}
		}
		
		return super.visit(name);
	}
		
	public IASTExpression transformEquals(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformNotEquals(pExpression, false, pTransPerformed);
		} else {
			return this.transformComparisonExpression(pExpression, "eq", IASTBinaryExpression.op_equals);
		}
	}
	
	public IASTExpression transformGreaterThan(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformLessEquals(pExpression, false, pTransPerformed);
		} else {
			return this.transformComparisonExpression(pExpression, "gt", IASTBinaryExpression.op_greaterThan);
		}
	}
	
	public IASTExpression transformGreaterEquals(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformLessThan(pExpression, false, pTransPerformed);
		} else {
			return this.transformComparisonExpression(pExpression, "ge", IASTBinaryExpression.op_greaterEqual);
		}
	}
	
	public IASTExpression transformLessThan(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformGreaterEquals(pExpression, false, pTransPerformed);
		} else {
			return this.transformComparisonExpression(pExpression, "lt", IASTBinaryExpression.op_lessThan);
		}
	}
	
	public IASTExpression transformLessEquals(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformGreaterThan(pExpression, false, pTransPerformed);
		} else {
			return this.transformComparisonExpression(pExpression, "le", IASTBinaryExpression.op_lessEqual);
		}
	}
	
	public IASTExpression transformNotEquals(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformEquals(pExpression, false, pTransPerformed);
		} else {
			return this.transformComparisonExpression(pExpression, "neq", IASTBinaryExpression.op_notequals);
		}
	}
	
	public IASTExpression transformAnd(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformLogicalExpression(pExpression, true, "or", IASTBinaryExpression.op_logicalOr);
		} else {
			return this.transformLogicalExpression(pExpression, false, "and", IASTBinaryExpression.op_logicalAnd);
		}
	}
	
	public IASTExpression transformOr(IASTBinaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		if (pNegation) {
			return this.transformLogicalExpression(pExpression, true, "and", IASTBinaryExpression.op_logicalAnd);
		} else {
			return this.transformLogicalExpression(pExpression, false, "or", IASTBinaryExpression.op_logicalOr);
		}
	}
	
	public IASTExpression transformNot(IASTUnaryExpression pExpression, boolean pNegation, boolean pTransPerformed) {
		IASTExpression operand = pExpression.getOperand();
		return this.transformDistanceExpression(operand, !pNegation, pTransPerformed);
	}
	
	public IASTExpression trasformArithmetic(IASTBinaryExpression pExpression, boolean pNegation, int operation) {
		if (pNegation) {
			return this.transformArithmeticExpression(pExpression, true, operation);
		} else {
			return this.transformArithmeticExpression(pExpression, false, operation);
		}
	}
	
	public IASTExpression transformDistanceExpression(IASTExpression expression, boolean pNegation, boolean pTransPerformed) {
		if (expression instanceof IASTBinaryExpression) {
			IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
			int binaryOperator = binaryExpression.getOperator();
			switch (binaryOperator) {
			case IASTBinaryExpression.op_equals:
				return this.transformEquals(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_greaterThan:
				return this.transformGreaterThan(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_greaterEqual:
				return this.transformGreaterEquals(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_lessThan:
				return this.transformLessThan(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_lessEqual:
				return this.transformLessEquals(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_notequals:
				return this.transformNotEquals(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_logicalAnd:
				return this.transformAnd(binaryExpression, pNegation, pTransPerformed);
			case IASTBinaryExpression.op_logicalOr:
				return this.transformOr(binaryExpression, pNegation, pTransPerformed);
			//changing for operators
			case IASTBinaryExpression.op_minus:
				return this.trasformArithmetic(binaryExpression, pNegation, IASTBinaryExpression.op_minus);
			case IASTBinaryExpression.op_plus:
				return this.trasformArithmetic(binaryExpression, pNegation, IASTBinaryExpression.op_plus);
			case IASTBinaryExpression.op_multiply:
				return this.trasformArithmetic(binaryExpression, pNegation, IASTBinaryExpression.op_multiply);
			case IASTBinaryExpression.op_binaryOr:
				return this.trasformArithmetic(binaryExpression, pNegation, IASTBinaryExpression.op_binaryOr);
			case IASTBinaryExpression.op_binaryAnd:
				return this.trasformArithmetic(binaryExpression, pNegation, IASTBinaryExpression.op_binaryAnd);
			//assignment operators
			case IASTBinaryExpression.op_assign: 
			case IASTBinaryExpression.op_plusAssign:
			case IASTBinaryExpression.op_minusAssign:
			case IASTBinaryExpression.op_binaryAndAssign:
			case IASTBinaryExpression.op_shiftLeftAssign:
			case IASTBinaryExpression.op_binaryOrAssign:
			case IASTBinaryExpression.op_binaryXorAssign:
			case IASTBinaryExpression.op_divideAssign:
			case IASTBinaryExpression.op_moduloAssign:
			case IASTBinaryExpression.op_multiplyAssign:
			case IASTBinaryExpression.op_shiftRightAssign:
				int binaryOperatorImplicit;
				switch (binaryOperator) {
				case IASTBinaryExpression.op_plusAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_plus;
					break;
				case IASTBinaryExpression.op_minusAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_minus;
					break;
				case IASTBinaryExpression.op_binaryAndAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_binaryAnd;
					break;
				case IASTBinaryExpression.op_shiftLeftAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_shiftLeft;
					break;
				case IASTBinaryExpression.op_binaryOrAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_binaryOr;
					break;
				case IASTBinaryExpression.op_binaryXorAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_binaryXor;
					break;
				case IASTBinaryExpression.op_divideAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_divide;
					break;
				case IASTBinaryExpression.op_moduloAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_modulo;
					break;
				case IASTBinaryExpression.op_multiplyAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_multiply;
					break;
				case IASTBinaryExpression.op_shiftRightAssign:
					binaryOperatorImplicit = IASTBinaryExpression.op_shiftRight;
					break;
				default:
					binaryOperatorImplicit = op_invalid;
				}
				IASTExpression expressionImplicit;
				if (binaryOperatorImplicit == op_invalid) {
					expressionImplicit = this.cloneExpression(binaryExpression.getOperand1());
				} else {
					expressionImplicit = new CASTBinaryExpression(
							binaryOperatorImplicit, 
							this.cloneExpression(binaryExpression.getOperand1()), 
							this.cloneExpression(binaryExpression.getOperand2()));
				}
				return this.transformDistanceExpression(expressionImplicit, pNegation, pTransPerformed);
				//TODO is this right? Shouldn't we also add the assignment?
			default:
				IASTExpression operand1 = binaryExpression.getOperand1();
				IASTExpression operand2 = binaryExpression.getOperand2();
				binaryExpression.setOperand1(this.transformDistanceExpression(operand1, pNegation, pTransPerformed));
				binaryExpression.setOperand2(this.transformDistanceExpression(operand2, pNegation, pTransPerformed));
				if (pTransPerformed) {
					return binaryExpression;
				} else {
					IASTExpression[] arguments = new IASTExpression[1];
					arguments[0] = binaryExpression;
					return makeFunctionCall("_f_ocelot_istrue", arguments);
				}
			}
		} else if (expression instanceof IASTUnaryExpression) {
			IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
			int unaryOperator = unaryExpression.getOperator();
			switch (unaryOperator) {
			case IASTUnaryExpression.op_not:
				return this.transformNot(unaryExpression, pNegation, pTransPerformed);
			case IASTUnaryExpression.op_amper:
			case IASTUnaryExpression.op_star:
				return unaryExpression;
			case IASTUnaryExpression.op_postFixDecr:
			case IASTUnaryExpression.op_postFixIncr:
				return this.transformDistanceExpression(unaryExpression.getOperand(), pNegation, pTransPerformed);
			case IASTUnaryExpression.op_prefixDecr: {
				IASTExpression expressionImplicit = new CASTBinaryExpression(
						IASTBinaryExpression.op_minus,
						this.cloneExpression(unaryExpression.getOperand()),
						new CASTLiteralExpression(IASTLiteralExpression.lk_integer_constant, new char[] {'1'}));
				return this.transformDistanceExpression(expressionImplicit, pNegation, pTransPerformed);
			}
			case IASTUnaryExpression.op_prefixIncr: {
				IASTExpression expressionImplicit = new CASTBinaryExpression(
						IASTBinaryExpression.op_plus,
						this.cloneExpression(unaryExpression.getOperand()),
						new CASTLiteralExpression(IASTLiteralExpression.lk_integer_constant, new char[] {'1'}));
				return this.transformDistanceExpression(expressionImplicit, pNegation, pTransPerformed);
			}
//			case IASTUnaryExpression.op_star:				
//				CASTCastExpression cast = new CASTCastExpression(type, this.cloneExpression(unaryExpression.getOperand()));
//				unaryExpression.setOperand(cast);
//				System.out.println(new ASTWriter().write(realExpression));
//				return unaryExpression;
			default:
				IASTExpression operand = unaryExpression.getOperand();
				return this.transformDistanceExpression(operand, pNegation, pTransPerformed);
			}
		} else if (expression instanceof IASTIdExpression ||
				expression instanceof IASTLiteralExpression ||
				expression instanceof IASTArraySubscriptExpression ||
				expression instanceof IASTConditionalExpression ||
				expression instanceof IASTCastExpression ||
				expression instanceof IASTFieldReference ||
				expression instanceof IASTTypeIdExpression) {
			if (!pTransPerformed) {
				IASTExpression[] arguments = new IASTExpression[1];
				arguments[0] = expression; 
				IASTExpression result;
				if (pNegation) {
					result = makeFunctionCall("_f_ocelot_isfalse", arguments);
				} else {
					result = makeFunctionCall("_f_ocelot_istrue", arguments);
				}
				return result;
			} 
		} else if (expression instanceof IASTFunctionCallExpression) { 
			return makeFunctionCall("_f_ocelot_get_fcall", new IASTExpression[0]);
		} else {
			try {
				throw new Exception("ERROR: Unhandled expression of type " + expression.getClass().getName());
			} catch (Exception e) {
				System.err.println(expression.getRawSignature());
				e.printStackTrace();
			}
		}

		return expression;
	}
	
	public IASTExpression transformOriginalExpression(IASTExpression expression) {
		if (expression instanceof IASTBinaryExpression) {
			IASTBinaryExpression realExpression = (IASTBinaryExpression)expression;
			realExpression.setOperand1(this.transformOriginalExpression(realExpression.getOperand1()));
			realExpression.setOperand2(this.transformOriginalExpression(realExpression.getOperand2()));
			return realExpression;
		} else if (expression instanceof IASTUnaryExpression) {
			IASTUnaryExpression realExpression = (IASTUnaryExpression)expression;
			realExpression.setOperand(this.transformOriginalExpression(realExpression.getOperand()));
			return realExpression;
		} else if (expression instanceof IASTFunctionCallExpression) {
			return this.registerFcallExpression(expression, 3);
		}
		return expression;
	}
	
	@Override
	public int visit(IASTExpression expression) {
		//this.transformDistanceExpression(expression, false, false);
		return PROCESS_SKIP;
	}
	
	//TODO start from here
	public void visit(IASTIfStatement statement) {
		IASTExpression[] instrArgs = new IASTExpression[3];
		instrArgs[0] = this.transformOriginalExpression(statement.getConditionExpression().copy());
		instrArgs[1] = this.transformDistanceExpression(this.cloneExpression(statement.getConditionExpression()), false, false);
		instrArgs[2] = this.transformDistanceExpression(this.cloneExpression(statement.getConditionExpression()), true, false);
		
		IASTFunctionCallExpression instrFunction = makeFunctionCall("_f_ocelot_trace", instrArgs);
		IASTExpression resultExpression = buildFcallExpression(instrFunction);
		
		statement.setConditionExpression(resultExpression);
	}
	
	public void visit(IASTSwitchStatement statement) {
		//OK, but handle types!! 
		CASTLiteralExpression cTrue = new CASTLiteralExpression(CASTLiteralExpression.lk_integer_constant, new char[] {'1'});
		CASTCompoundStatement substitute = new CASTCompoundStatement();
		this.switchExpressions.push(new ArrayList<IASTStatement>());
		statement.getBody().accept(this);
		List<IASTStatement> caseStatements = this.switchExpressions.pop();
		CASTBinaryExpression defaultExpression = new CASTBinaryExpression(
				CASTBinaryExpression.op_logicalAnd, 
				cTrue.copy(), 
				cTrue.copy()
		);
		
		IASTExpression switchExpression = statement.getControllerExpression();
		
		if (switchExpression instanceof IASTFunctionCallExpression) {
			int countTotalRegisters = 0;
			
			for (IASTStatement aCase : caseStatements) {
				if (aCase instanceof IASTCaseStatement) {
					countTotalRegisters += 4;
				}
			}
			
			switchExpression = this.registerFcallExpression(
					statement.getControllerExpression(), 
					countTotalRegisters);
			
			
			for (IASTExpression callExpression : this.functionCallsInExpressions) {
				IASTStatement registerFcall = new CASTExpressionStatement(callExpression);
				substitute.addStatement(registerFcall);
			}
		}
		
		CASTBinaryExpression currentDefaultExpression = defaultExpression;
		
		boolean defaultWritten = false;
		for (IASTStatement aCase : caseStatements) {
			IASTExpression distanceCalculation;
			String label;
			
			if (aCase instanceof IASTCaseStatement) {
				IASTCaseStatement realCase = (IASTCaseStatement)aCase;
				
				label = new ASTWriter().write(realCase.getExpression());
				
				distanceCalculation = new CASTBinaryExpression(
						CASTBinaryExpression.op_equals,
						this.cloneExpression(switchExpression), 
						this.cloneExpression(realCase.getExpression())
				);
				
				//Creates an AND on with the != on the left and a "true"
				CASTBinaryExpression defaultExpressionSubtree = 
						new CASTBinaryExpression(
								CASTBinaryExpression.op_logicalAnd, 
								new CASTBinaryExpression(
										CASTBinaryExpression.op_notequals, 
										this.cloneExpression(switchExpression), 
										this.cloneExpression(realCase.getExpression())
								), 
								cTrue.copy()
						);
				
				currentDefaultExpression.setOperand2(defaultExpressionSubtree);
				currentDefaultExpression = defaultExpressionSubtree;
			} else {
				defaultWritten = true;
				label = "default";
				distanceCalculation = defaultExpression;
			}
			
			IASTExpression[] arguments = new IASTExpression[3];
			arguments[0] = new CASTLiteralExpression(CASTLiteralExpression.lk_integer_constant, String.valueOf(CaseEdge.retrieveUniqueId(label)).toCharArray());
			arguments[1] = this.transformDistanceExpression(distanceCalculation, false, false);
			arguments[2] = distanceCalculation.copy();
			
			substitute.addStatement(new CASTExpressionStatement(makeFunctionCall("_f_ocelot_trace_case", arguments)));
		}
		
		if (!defaultWritten) {
			String label = "default";
			IASTExpression distanceCalculation = defaultExpression;
			
			IASTExpression[] arguments = new IASTExpression[3];
			arguments[0] = new CASTLiteralExpression(CASTLiteralExpression.lk_integer_constant, String.valueOf(CaseEdge.retrieveUniqueId(label)).toCharArray());
			arguments[1] = this.transformDistanceExpression(distanceCalculation, false, false);
			arguments[2] = distanceCalculation.copy();
			
			substitute.addStatement(new CASTExpressionStatement(makeFunctionCall("_f_ocelot_trace_case", arguments)));
		}
		
		IASTNode parent = statement.getParent();
		
		if (parent instanceof IASTStatement) {
			if (parent instanceof IASTCompoundStatement) {
				CASTCompoundStatement realParent = (CASTCompoundStatement)parent;
				realParent.replace(statement, substitute);
				//for (int i = 0; i < realParent.getStatements().length; i++)
				//	if (statement == realParent.getStatements()[i])
						
			} else if (parent instanceof IASTIfStatement) {
				IASTIfStatement realParent = (IASTIfStatement)parent;
				if (statement == realParent.getThenClause())
					realParent.setThenClause(substitute);
				else
					realParent.setElseClause(substitute);
			} else if (parent instanceof IASTSwitchStatement) {
				IASTSwitchStatement realParent = (IASTSwitchStatement)parent;
				realParent.setBody(substitute);
			} else if (parent instanceof IASTWhileStatement) {
				IASTWhileStatement realParent = (IASTWhileStatement)parent;
				realParent.setBody(substitute);
			} else if (parent instanceof IASTDoStatement) {
				IASTDoStatement realParent = (IASTDoStatement)parent;
				realParent.setBody(substitute);
			} else if (parent instanceof IASTForStatement) {
				IASTForStatement realParent = (IASTForStatement)parent;
				if (statement == realParent.getInitializerStatement())
					realParent.setInitializerStatement(substitute);
				else
					realParent.setBody(substitute);
			}
		}
		
		substitute.addStatement(statement);
	}
	
	public void visit(IASTWhileStatement statement) {
		IASTExpression instrArg1 = this.transformOriginalExpression(statement.getCondition().copy());
		IASTExpression instrArg2 = this.transformDistanceExpression(this.cloneExpression(statement.getCondition()), false, false);
		IASTExpression instrArg3 = this.transformDistanceExpression(this.cloneExpression(statement.getCondition()), true, false);
		
		IASTFunctionCallExpression instrFunction = makeFunctionCall("_f_ocelot_trace", instrArg1, instrArg2, instrArg3);
		IASTExpression resultExpression = this.buildFcallExpression(instrFunction);
		statement.setCondition(resultExpression);
	}
	
	public void visit(IASTDoStatement statement) {
		IASTExpression[] instrArgs = new IASTExpression[3];
		instrArgs[0] = this.transformOriginalExpression(statement.getCondition().copy());
		instrArgs[1] = this.transformDistanceExpression(this.cloneExpression(statement.getCondition()), false, false);
		instrArgs[2] = this.transformDistanceExpression(this.cloneExpression(statement.getCondition()), true, false);
		
		IASTFunctionCallExpression instrFunction = makeFunctionCall("_f_ocelot_trace", instrArgs);
		IASTExpression resultExpression = this.buildFcallExpression(instrFunction);
		statement.setCondition(resultExpression);
	}
	
	public void visit(IASTForStatement statement) {
		IASTExpression[] instrArgs = new IASTExpression[3];
		instrArgs[0] = this.transformOriginalExpression(statement.getConditionExpression().copy());
		instrArgs[1] = this.transformDistanceExpression(this.cloneExpression(statement.getConditionExpression()), false, false);
		instrArgs[2] = this.transformDistanceExpression(this.cloneExpression(statement.getConditionExpression()), true, false);
		
		IASTFunctionCallExpression instrFunction = makeFunctionCall("_f_ocelot_trace", instrArgs);
		IASTExpression resultExpression = this.buildFcallExpression(instrFunction);
		statement.setConditionExpression(resultExpression);
	}
	
	public void visit(IASTCaseStatement statement) {
		this.switchExpressions.lastElement().add(statement);
	}
	
	public void visit(IASTDefaultStatement statement) {
		this.switchExpressions.lastElement().add(statement);
	}
	
	public int visit(IASTStatement statement) {
		this.functionCallsInExpressions.clear();
		
		if (statement instanceof IASTIfStatement) {
			this.visit((IASTIfStatement) statement);
		} else if (statement instanceof IASTSwitchStatement) {
			this.visit((IASTSwitchStatement) statement);
			return PROCESS_SKIP; //Visits the statement on its own!
		} else if (statement instanceof IASTWhileStatement) {
			this.visit((IASTWhileStatement) statement);
		} else if (statement instanceof IASTDoStatement) {
			this.visit((IASTDoStatement) statement);
		} else if (statement instanceof IASTForStatement) {
			this.visit((IASTForStatement) statement);
		} else if (statement instanceof IASTCaseStatement) {
			this.visit((IASTCaseStatement) statement);
		} else if (statement instanceof IASTDefaultStatement) {
			this.visit((IASTDefaultStatement) statement);
		}
		
		return PROCESS_CONTINUE;
	}
	
	private IASTFunctionCallExpression makeFunctionCall(String pName, IASTExpression... pArguments) {
		IASTFunctionCallExpression call = new CASTFunctionCallExpression();
		IASTIdExpression name = new CASTIdExpression(new CASTName(pName.toCharArray()));
		call.setFunctionNameExpression(name);
		call.setArguments(pArguments);
		return call;
	}
	
	private IASTExpression transformLogicalExpression(IASTBinaryExpression pExpression, boolean pNegation, String pOperator, int pRealOperator) {
		pExpression.setOperator(pRealOperator);
		IASTExpression op1 = pExpression.getOperand1();
		IASTExpression op2 = pExpression.getOperand2();
		IASTExpression instrumentedOp1 = this.transformDistanceExpression(op1, pNegation, false);
		IASTExpression instrumentedOp2 = this.transformDistanceExpression(op2, pNegation, false);
		
//		System.out.println("FROM:" + new ASTWriter().write(pExpression));
//		System.out.println("TO:" + new ASTWriter().write(operationArgs[0]) + " " + pOperator +" " + new ASTWriter().write(operationArgs[1]));
		
		IASTFunctionCallExpression operationFunction = makeFunctionCall("_f_ocelot_" + pOperator, instrumentedOp1, instrumentedOp2);
		return operationFunction;
	}
	
	/**
	 * Returns the correspondent {@link IASTFunctionCallExpression} needed
	 * for the instrumentation, for a given {@link IASTBinaryExpression}
	 * passed as parameter.
	 * 
	 * @param pExpression the expression 
	 * @param negation flag for the negation of the expression
	 * @param pRealOperator the operator 
	 * @return a {@link IASTFunctionCallExpression}.
	 */
	private IASTExpression transformArithmeticExpression(IASTBinaryExpression pExpression, boolean negation, int pRealOperator) {
		pExpression.setOperator(pRealOperator);
		IASTExpression operand1 = pExpression.getOperand1();
		IASTExpression operand2 = pExpression.getOperand2();
		
		IASTExpression instrumentedOp1 = this.transformDistanceExpression(operand1, false, true);
		IASTExpression instrumentedOp2 = this.transformDistanceExpression(operand2, true, true);
				
		IASTBinaryExpression auxExpression = pExpression.copy();
		auxExpression.setOperand1(instrumentedOp1);
		auxExpression.setOperand2(instrumentedOp2);
				
		IASTExpression result;
		if (negation) {
			result = makeFunctionCall("_f_ocelot_istrue", auxExpression);
		} else {
			result = makeFunctionCall("_f_ocelot_isfalse", auxExpression);
		}
		return result;
	}
	
	private IASTExpression transformComparisonExpression(IASTBinaryExpression pExpression, String pOperator, int pRealOperator) {
		pExpression.setOperator(pRealOperator);
		IASTExpression operand1 = pExpression.getOperand1();
		IASTExpression operand2 = pExpression.getOperand2();
		
		IType op1Type = getType(operand1);
		IType op2Type = getType(operand2);
		
		IASTExpression instrumentedOp1 = this.castToDouble(this.transformDistanceExpression(operand1, false, true));
		IASTExpression instrumentedOp2 = this.castToDouble(this.transformDistanceExpression(operand2, false, true));
		
//		System.out.println("FROM:" + new ASTWriter().write(pExpression));
//		System.out.println("TO:" + new ASTWriter().write(operationArgs[0]) + " " + pOperator +" " + new ASTWriter().write(operationArgs[1]));
		
		IASTFunctionCallExpression operationFunction;
		if (op1Type instanceof IBasicType && op2Type instanceof IBasicType ||
			op1Type instanceof IEnumeration && op2Type instanceof IEnumeration) {
			operationFunction = makeFunctionCall("_f_ocelot_" + pOperator + "_numeric", instrumentedOp1, instrumentedOp2);
		} else if (op1Type instanceof ICPointerType && op2Type instanceof ICPointerType) {
			operationFunction = makeFunctionCall("_f_ocelot_" + pOperator + "_pointer", instrumentedOp1, instrumentedOp2);
		} else {
			System.err.println("NOTE: assuming argument numeric. Fix for types: " + op1Type.getClass().getSimpleName() + "-" + op2Type.getClass().getSimpleName());
			System.err.println("NOTE: origin: " + operand1.getRawSignature() + " --- " + operand2.getRawSignature());
			operationFunction = makeFunctionCall("_f_ocelot_" + pOperator + "_numeric", instrumentedOp1, instrumentedOp2);
		}
		
		return operationFunction;
	}
	
	private IASTExpression registerFcallExpression(IASTExpression expression, int pHowMany) {
		String howMany = String.valueOf(pHowMany);
		IASTExpression argument1 = expression; 
		IASTExpression argument2 = new CASTLiteralExpression(CASTLiteralExpression.lk_integer_constant, howMany.toCharArray());
		
		IType type = getType(expression);
		if (type instanceof IBasicType) {
			this.functionCallsInExpressions.add(makeFunctionCall("_f_ocelot_reg_fcall_numeric", argument1, argument2));
			return makeFunctionCall("_f_ocelot_get_fcall");
		} else if (type instanceof ICPointerType) {
			this.functionCallsInExpressions.add(makeFunctionCall("_f_ocelot_reg_fcall_pointer", argument1, argument2));
			return makeFunctionCall("_f_ocelot_get_fcall");
		} else {
			ASTWriter writer = new ASTWriter();
			System.err.println("I can't handle this type situation: " + type.toString() + ". Assuming numeric. Please, manually check.");
			System.err.println("Error node: " + writer.write(expression.getParent()));
			this.functionCallsInExpressions.add(makeFunctionCall("_f_ocelot_reg_fcall_numeric", argument1, argument2));
			return makeFunctionCall("_f_ocelot_get_fcall");
		}
	}
	
	private IASTExpression buildFcallExpression(IASTExpression instrFunction) {
		IASTExpression resultExpression;
		
		if (this.functionCallsInExpressions.size() > 0) {
			resultExpression = new CASTBinaryExpression(
					IASTBinaryExpression.op_logicalAnd, 
					this.functionCallsInExpressions.get(this.functionCallsInExpressions.size() - 1), 
					instrFunction);
			if (this.functionCallsInExpressions.size() > 1) {
				for (int i = this.functionCallsInExpressions.size() - 2; i >= 0; i--) {
					IASTExpression superFcall = new CASTBinaryExpression(
							IASTBinaryExpression.op_logicalAnd,
							this.functionCallsInExpressions.get(i),
							resultExpression);
					resultExpression = superFcall;
				}
			}			
		} else {
			resultExpression = instrFunction;
		}
		
		return resultExpression;
	}
	
	private IASTExpression cloneExpression(IASTExpression pExpression) {
		IASTExpression copy = pExpression.copy();
		copy.setParent(pExpression.getParent());
		copy.setPropertyInParent(pExpression.getPropertyInParent());
		
		if (copy instanceof IASTBinaryExpression) {
			IASTBinaryExpression realCopy = (IASTBinaryExpression) copy;
			IASTBinaryExpression realOrig = (IASTBinaryExpression) pExpression;
			realCopy.setOperand1(realOrig.getOperand1());
			realCopy.setOperand2(realOrig.getOperand2());
		} else if (copy instanceof IASTUnaryExpression) {
			IASTUnaryExpression realCopy = (IASTUnaryExpression) copy;
			IASTUnaryExpression realOrig = (IASTUnaryExpression) pExpression;
			realCopy.setOperand(realOrig.getOperand());
		} else if (copy instanceof IASTIdExpression) {
			IASTIdExpression realCopy = (IASTIdExpression) copy;
			IASTIdExpression realOrig = (IASTIdExpression) pExpression;
			realCopy.getName().setBinding(realOrig.getName().getBinding());
		} else if (copy instanceof IASTArraySubscriptExpression) {
			IASTArraySubscriptExpression realCopy = (IASTArraySubscriptExpression) copy;
			IASTArraySubscriptExpression realOrig = (IASTArraySubscriptExpression) pExpression;
			realCopy.setArrayExpression(realOrig.getArrayExpression());
			realCopy.setArgument(realOrig.getArgument());
		} //TODO else?
		
		return copy;
	}
	
	private IASTExpression castToDouble(IASTExpression pExpression) {
		//IASTCastExpression cast = new CASTCastExpression(new CASTTypeId(null, new CASTDeclarator(new CASTName("double".toCharArray()))), pExpression);
		
		return pExpression;
	}
	
	private IType getType(IASTExpression pExpression) {
//		System.out.println(pExpression.getRawSignature());
		return getType(pExpression.getExpressionType());
	}
	
	private IType getType(IType type) {
		IType retVal = type;
		while (retVal instanceof CTypedef) {
			retVal = ((CTypedef) retVal).getType();
		}
		return retVal;
	}
		
	// Post-visit results getters
	
	public List<IASTNode> getTypedefs() {
		return this.typedefs;
	}
}
