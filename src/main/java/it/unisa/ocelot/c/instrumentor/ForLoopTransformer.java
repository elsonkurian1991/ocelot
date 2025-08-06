package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.internal.core.dom.parser.c.*;
import org.eclipse.cdt.internal.core.dom.rewrite.astwriter.ASTWriter;

public class ForLoopTransformer extends ASTVisitor {
	private final IASTTranslationUnit tu;
	private int loopCounter = 1;
	private final ICNodeFactory nodeFactory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
	private final Set<String> trackedBoolVars = new HashSet<>();
	public Set<IASTNode> trackSynthetics = new HashSet<>();
	public ForLoopTransformer(IASTTranslationUnit tu) {

		super();
		this.tu=tu;
		this.shouldVisitStatements = true;


	}
	@Override
	public int visit(IASTStatement statement) {
		if (statement instanceof IASTForStatement) {
			IASTForStatement forStmt = (IASTForStatement) statement;
			IASTNode parent = forStmt.getParent();

			if (!(parent instanceof IASTCompoundStatement)) {
				System.err.println(parent.getRawSignature());
				return PROCESS_CONTINUE;
			}
			IASTCompoundStatement compoundParent = (IASTCompoundStatement) parent;
			String counterName = "forLoopCount_" + loopCounter;
			ASTWriter writer = new ASTWriter();
			// 1. Create declaration: int forLoopCount_N = 0;
			IASTDeclarationStatement declStmt = createDeclarationStatement(counterName);
			System.out.println("Inserted declaration:\n" + writer.write(declStmt));

			// 2. Create increment: forLoopCount_N += 1;
			IASTExpressionStatement incrementStmt = createIncrement(counterName);
			//System.out.println("Inserted increment:\n" + writer.write(incrementStmt));

			// 3. Inject increment into loop body
			IASTStatement originalBody = forStmt.getBody();
			IASTCompoundStatement newLoopBody = nodeFactory.newCompoundStatement();
			newLoopBody.addStatement(incrementStmt);

			if (originalBody instanceof IASTCompoundStatement) {
				IASTCompoundStatement originalCompound = (IASTCompoundStatement) originalBody;
				for (IASTStatement s : originalCompound.getStatements()) {
					newLoopBody.addStatement(s);
				}
			} else {
				newLoopBody.addStatement(originalBody);
			}

			forStmt.setBody(newLoopBody);

			// 4. Create post-loop check
			IASTIfStatement postCheck = createPostLoopCheck(counterName);
			//System.out.println("Inserted post-check:\n" + writer.write(postCheck));
			
			// 5. Rebuild compound block
			IASTCompoundStatement newCompound = nodeFactory.newCompoundStatement();
			IASTStatement[] stmts = compoundParent.getStatements();
			
			for (IASTStatement s : stmts) {
				if (s == forStmt) {
					newCompound.addStatement(declStmt);
					newCompound.addStatement(forStmt);
					newCompound.addStatement(postCheck);
					trackSynthetics.add(postCheck);
				} else {
					newCompound.addStatement(s);
				}
			}
			postCheck.setPropertyInParent(new ASTNodeProperty("GENERATED"));//remove this if condition to calculate coverage.
			//System.out.println("newCompound:\n" + writer.write(newCompound));
			/* 6. Replace in parent (assumes parent is function body)
            IASTNode grandParent = compoundParent.getParent();
            if (grandParent instanceof IASTFunctionDefinition) {
                IASTFunctionDefinition funcDef = (IASTFunctionDefinition) grandParent;
                funcDef.setBody(newCompound);
                System.out.println("funcDef:\n" + writer.write(funcDef));
            }*/
			// 6. Replace old compound with the modified one (newCompound), in its parent
			//TODO
			IASTNode grandParent = compoundParent.getParent();
			if (grandParent instanceof IASTStatement) {
				if (grandParent instanceof IASTCompoundStatement) {
					System.out.println("IASTCompoundStatement");
					IASTCompoundStatement gpCompound = (IASTCompoundStatement) grandParent;
					IASTStatement[] norstmts = gpCompound.getStatements();
					for (int i = 0; i < norstmts.length; i++) {
						if (norstmts[i] == compoundParent) {
							norstmts[i] = newCompound;  // replace
							break;
						}
					}
				} else if (grandParent instanceof IASTIfStatement) {
					System.out.println("IASTIfStatement");
					IASTIfStatement ifStmt = (IASTIfStatement) grandParent;
					if (ifStmt.getThenClause() == compoundParent) {
						ifStmt.setThenClause(newCompound);
					} else if (ifStmt.getElseClause() == compoundParent) {
						ifStmt.setElseClause(newCompound);
					}
				} else if (grandParent instanceof IASTWhileStatement) {
					System.out.println("IASTWhileStatement");
					IASTWhileStatement whileStmt = (IASTWhileStatement) grandParent;
					if (whileStmt.getBody() == compoundParent) {
						whileStmt.setBody(newCompound);
					}
				} else if (grandParent instanceof IASTForStatement) {
					System.out.println("IASTForStatement");
					IASTForStatement loop = (IASTForStatement) grandParent;
					if (loop.getBody() == compoundParent) {
						loop.setBody(newCompound);
					}
				} else if (grandParent instanceof IASTDoStatement) {
					System.out.println("IASTDoStatement");
					IASTDoStatement doStmt = (IASTDoStatement) grandParent;
					if (doStmt.getBody() == compoundParent) {
						doStmt.setBody(newCompound);
					}
				} else {
					System.err.println("Unsupported parent statement: " + grandParent.getClass().getSimpleName());
				}
			} else if (grandParent instanceof IASTFunctionDefinition) {
				System.out.println("IASTFunctionDefinition");
				IASTFunctionDefinition funcDef = (IASTFunctionDefinition) grandParent;
				funcDef.setBody(newCompound);
			} else {
				System.err.println("Unsupported parent node: " + grandParent.getClass().getSimpleName());
			}
			loopCounter++;

		}

		return PROCESS_CONTINUE;
	}

	// Helper: int forLoopCount_N = 0;
	private IASTDeclarationStatement createDeclarationStatement(String name) {
		IASTSimpleDeclSpecifier declSpec = nodeFactory.newSimpleDeclSpecifier();
		declSpec.setType(IASTSimpleDeclSpecifier.t_double);

		IASTDeclarator declarator = nodeFactory.newDeclarator(nodeFactory.newName(name.toCharArray()));
		IASTInitializer initializer = nodeFactory.newEqualsInitializer(
				nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_float_constant, "0.0")
				);
		declarator.setInitializer(initializer);

		IASTSimpleDeclaration declaration = nodeFactory.newSimpleDeclaration(declSpec);
		declaration.addDeclarator(declarator);

		IASTDeclarationStatement stmt = nodeFactory.newDeclarationStatement(declaration);
		return stmt;
	}

	// Helper: forLoopCount_N += 1;
	private IASTExpressionStatement createIncrement(String name) {
		IASTIdExpression var = new CASTIdExpression(new CASTName(name.toCharArray()));
		IASTLiteralExpression one = new CASTLiteralExpression(IASTLiteralExpression.lk_float_constant, "1".toCharArray());
		IASTBinaryExpression expr = new CASTBinaryExpression(IASTBinaryExpression.op_plusAssign, var, one);
		return new CASTExpressionStatement(expr);
	}

	private IASTIfStatement createPostLoopCheck(String name) {
		// Create type specifier for 'double'
		IASTSimpleDeclSpecifier doubleSpec = nodeFactory.newSimpleDeclSpecifier();
		doubleSpec.setType(IASTSimpleDeclSpecifier.t_double);
		// Wrap it in a TypeId
		IASTTypeId typeId = nodeFactory.newTypeId(doubleSpec, null);

		// Create first cast expression: (double)name
		IASTIdExpression nameExpr1 = nodeFactory.newIdExpression(nodeFactory.newName(name.toCharArray()));
		//IASTCastExpression castExpr1 = nodeFactory.newCastExpression(IASTCastExpression.op_cast, typeId, nameExpr1);

		// Create second cast expression: (double)name
		IASTIdExpression nameExpr2 = nodeFactory.newIdExpression(nodeFactory.newName(name.toCharArray()));
		//IASTCastExpression castExpr2 = nodeFactory.newCastExpression(IASTCastExpression.op_cast, typeId, nameExpr2);

		// Create literal: 1
		IASTLiteralExpression one = nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_integer_constant, "1");

		// Create parentheses around the denominator: ((double)name + 1)
		IASTBinaryExpression denominatorAddition = nodeFactory.newBinaryExpression(IASTBinaryExpression.op_plus, nameExpr2, one);
		// Wrap the addition in parentheses using an IASTUnaryExpression with parentheses
		IASTUnaryExpression parenthesizedDenominator = nodeFactory.newUnaryExpression(IASTUnaryExpression.op_bracketedPrimary, denominatorAddition);

		// Create division: (double)name / ((double)name + 1)
		IASTBinaryExpression division = nodeFactory.newBinaryExpression(IASTBinaryExpression.op_divide, nameExpr1, parenthesizedDenominator);

		// Create threshold: 0.98
		IASTLiteralExpression threshold = nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_float_constant, "0.98");

		// Create condition: division >= 0.98
		IASTBinaryExpression condition = nodeFactory.newBinaryExpression(IASTBinaryExpression.op_greaterEqual, division, threshold);

		// Create body: name = name;
		IASTIdExpression lhs = nodeFactory.newIdExpression(nodeFactory.newName(name.toCharArray()));
		IASTIdExpression rhs = nodeFactory.newIdExpression(nodeFactory.newName(name.toCharArray()));
		IASTBinaryExpression assignment = nodeFactory.newBinaryExpression(IASTBinaryExpression.op_assign, lhs, rhs);
		IASTExpressionStatement assignmentStmt = nodeFactory.newExpressionStatement(assignment);

		// Create compound statement body
		IASTCompoundStatement body = nodeFactory.newCompoundStatement();
		body.addStatement(assignmentStmt);

		// Create and return the if statement
		IASTIfStatement ifStmt = nodeFactory.newIfStatement(condition, body, null);
		return ifStmt;
	}

}
	