package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aspectj.org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.INodeFactory;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTModification;
import org.eclipse.cdt.internal.core.dom.rewrite.ASTModificationStore;
import org.eclipse.text.edits.TextEditGroup;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGVisitor;

public class InstrumenterVisitForIfMethodCalls extends ASTVisitor{
	private final String functionName;
	private final ASTRewrite rewriter;
	private int booleanVariableCounter = 0;


	public InstrumenterVisitForIfMethodCalls(String pInstrumentFunction,ASTRewrite rewriter) {
		this.shouldVisitExpressions = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitPointerOperators = true;
		this.functionName = pInstrumentFunction;
		this.rewriter = rewriter;
	}
	@Override
	public int visit(IASTTranslationUnit tu) {
		/*
		 * Simulates the generation of a CFG in order to correctly retrieve the "case"
		 * unique ids. The resulting CFG will never be used. TODO define a lightweight
		 * visitor able to initialize the "case" unique ids only correctly.
		 */
		tu.accept(new CFGVisitor(new CFG(), this.functionName));

		return super.visit(tu);
	}
	private boolean containsMethodCall(IASTExpression expression) {
		if (expression == null) {
			return false;
		}

		// Direct method call check
		if (expression instanceof ICPPASTFunctionCallExpression || 
				expression instanceof IASTFunctionCallExpression) {
			return true;
		}

		// Recursively check child expressions
		for (IASTNode child : expression.getChildren()) {
			if (child instanceof IASTExpression && containsMethodCall((IASTExpression) child)) {
				return true;
			}
		}

		return false;
	}
	private String generateBooleanVariableName() {
		return "conditionResult_" + (++booleanVariableCounter);
	}
	private IASTDeclarationStatement createBooleanDeclaration(String varName, IASTExpression initExpression, 
			IASTTranslationUnit translationUnit) {
		INodeFactory factory = translationUnit.getASTNodeFactory();

		// Create boolean type specifier
		IASTSimpleDeclSpecifier boolType = factory.newSimpleDeclSpecifier();
		boolType.setType(IASTSimpleDeclSpecifier.t_bool);

		// Create declarator with initializer
		IASTDeclarator declarator = factory.newDeclarator(factory.newName(varName.toCharArray()));
		IASTEqualsInitializer initializer = factory.newEqualsInitializer(initExpression.copy());
		declarator.setInitializer(initializer);

		// Create simple declaration
		IASTSimpleDeclaration simpleDecl = factory.newSimpleDeclaration(boolType);
		simpleDecl.addDeclarator(declarator);

		// Wrap in declaration statement
		return factory.newDeclarationStatement(simpleDecl);
	}
	private void instrumentIfCondition(IASTIfStatement ifStatement, IASTExpression originalCondition) {
		try {
			// Generate a unique boolean variable name
			String boolVarName = generateBooleanVariableName();

			// Get translation unit for creating new AST nodes
			IASTTranslationUnit translationUnit = ifStatement.getTranslationUnit();

			// Create boolean variable declaration with initialization
			IASTDeclarationStatement boolDeclaration = createBooleanDeclaration(boolVarName, originalCondition, translationUnit);

			// Create new condition using the boolean variable
			IASTIdExpression newCondition = createVariableReference(boolVarName, translationUnit);

			// Get the parent compound statement to insert the declaration
			IASTNode parent = ifStatement.getParent();
			if (parent instanceof IASTCompoundStatement) {
				IASTCompoundStatement compound = (IASTCompoundStatement) parent;

				

				// Replace the if-condition with the boolean variable reference
				ifStatement.setConditionExpression(newCondition);
				// Insert the boolean declaration before the if-statement
				insertDeclarationBefore(compound, ifStatement, boolDeclaration);
			}

		} catch (Exception e) {
			System.err.println("Error instrumenting if-condition: " + e.getMessage());
		}
	}

	private IASTIdExpression createVariableReference(String varName, IASTTranslationUnit translationUnit) {
		INodeFactory factory = translationUnit.getASTNodeFactory();
		IASTName name = factory.newName(varName.toCharArray());
		return factory.newIdExpression(name);
	}

	private void insertDeclarationBefore(IASTCompoundStatement compound, IASTStatement targetStatement, IASTDeclarationStatement declarationToInsert) {
	    // Get all current statements in the compound statement
	    IASTStatement[] statements = compound.getStatements();
	    
	    // Find the index of the target statement
	    int targetIndex = -1;
	    for (int i = 0; i < statements.length; i++) {
	        if (statements[i] == targetStatement) {
	            targetIndex = i;
	            break;
	        }
	    }
	    
	    // If target statement not found, throw an exception
	    if (targetIndex == -1) {
	        throw new IllegalArgumentException("Target statement not found in compound statement");
	    }
	    
	    // Create a new compound statement with the declaration inserted
	    IASTTranslationUnit translationUnit = compound.getTranslationUnit();
	    IASTCompoundStatement newCompound = translationUnit.getASTNodeFactory().newCompoundStatement();
	    
	    // Copy statements before the target
	    for (int i = 0; i < targetIndex; i++) {
	        newCompound.addStatement(statements[i].copy(CopyStyle.withLocations));
	    }
	    
	    // Insert the declaration
	    newCompound.addStatement(declarationToInsert);
	    
	    // Copy the remaining statements (including the target)
	    for (int i = targetIndex; i < statements.length; i++) {
	        newCompound.addStatement(statements[i].copy(CopyStyle.withLocations));
	    }
	    
	    // Replace the compound statement based on its parent type
	    IASTNode parent = compound.getParent();
	    if (parent instanceof IASTFunctionDefinition) {
	        ((IASTFunctionDefinition) parent).setBody(newCompound);
	    } else if (parent instanceof IASTIfStatement) {
	        IASTIfStatement ifStmt = (IASTIfStatement) parent;
	        if (ifStmt.getThenClause() == compound) {
	            ifStmt.setThenClause(newCompound);
	        } else if (ifStmt.getElseClause() == compound) {
	            ifStmt.setElseClause(newCompound);
	        }
	    } else if (parent instanceof IASTWhileStatement) {
	        ((IASTWhileStatement) parent).setBody(newCompound);
	    } else if (parent instanceof IASTForStatement) {
	        ((IASTForStatement) parent).setBody(newCompound);
	    } else if (parent instanceof IASTDoStatement) {
	        ((IASTDoStatement) parent).setBody(newCompound);
	    } else if (parent instanceof IASTSwitchStatement) {
	        ((IASTSwitchStatement) parent).setBody(newCompound);
	    } else {
	        throw new UnsupportedOperationException("Unsupported parent type: " + parent.getClass().getSimpleName());
	    }
	}
	/*private void insertDeclarationBefore(IASTCompoundStatement compound, IASTStatement target, 
			IASTDeclarationStatement declaration) {
		// Build new Compound statements
		IASTCompoundStatement newCompound = makeCompoundStatement("pass the new statement");

		// Make a copy of the original
		IASTCompoundStatement copyOfCompoundStatement = compound.copy();

		// Replace in compound block
		IASTCompoundStatement block = (IASTCompoundStatement)compound.getParent();

		// Replace the original with the new ones



		ASTRewrite rewriter = ASTRewrite.create(compound.getTranslationUnit());

		// Get the list rewriter for the compound statement
		ListRewrite listRewrite = rewriter.getListRewrite(block, 
				IASTCompoundStatement STATEMENTS_PROPERTY);

		// Remove original and add new statements
		listRewrite.remove(compound, null);
		listRewrite.insertLast(newCompound, null);
		listRewrite.insertLast(copyOfCompoundStatement, null);

		// Apply changes
		TextEdit edits = rewriter.rewriteAST();
		IASTCompoundStatement newBlock = compound.getTranslationUnit().getASTNodeFactory()
				.newCompoundStatement();
		for (IASTStatement stmt : block.getStatements()) {
			if(stmt ==compound) {
				newBlock.addStatement(newCompound.copy());
				newBlock.addStatement(copyOfCompoundStatement.copy());
			}
			else {
				newBlock.addStatement(stmt.copy());
			}
		}
		// Replace the entire block in its parent
		block.getParent().replace(block, newBlock);

		/* List<IASTStatement> stmts = new ArrayList<>(Arrays.asList(block.getStatements()));
        int index = stmts.indexOf(compound);
        stmts.remove(index);
        stmts.add(index, newCompound);
        stmts.add(index + 1, copyOfCompoundStatement);
       // stmts.add(index + 2, after);
        block.setStatements(stmts.toArray(new IASTStatement[0]));

		 */

		//IASTCompoundStatement newCompound = new CPPASTCompoundStatement();



/*

		IASTStatement[] statements = compound.getStatements();
		int idx=0;
		// Find the index of the target statement

		for (int i = 0; i < statements.length+1; i++) {

			if (statements[i] == target) {
				newCompound.addStatement(target.copy());
				i++;
				// Insert the declaration before the target statement
				idx=i;
				System.err.println("inside each"+i);
				System.out.println(statements[i].getRawSignature());
				//rewriter.insertBefore(compound, target, declaration, null);
				break;
			}
			newCompound.addStatement(statements[i].copy());
		}
		TextEditGroup editGroup = new TextEditGroup("Insert Statement");
		ASTRewrite rewrite1= ASTRewrite.create(compound.getTranslationUnit());
		rewrite1.insertBefore(compound, statements[idx-1], declaration,editGroup);
		//rewrite1.replace(compound, newCompound, null);


		/*IASTStatement[] statements1 = Arrays.copyOfRange(statements, 0, idx);
		IASTStatement[] statements2 = Arrays.copyOfRange(statements, idx, statements.length);
		IASTStatement[] newStatements= new IASTStatement[statements.length+1];
		System.arraycopy(statements1, 0, newStatements, 0, statements1.length);
		newStatements[statements1.length] = declaration;
		System.arraycopy(statements2, 0, newStatements, statements1.length+1, statements2.length);
		System.out.println(newStatements.length);

		for(int i=0;i<newStatements.length;i++) {
			newCompound.addStatement(newStatements[i]);
			System.out.println(statements1.length);
			System.out.println(i);
			System.out.println(newStatements[i].getRawSignature());
		}*/

		//newCompound.setParent(compound.getParent());
		//target.setParent(newCompound);
		/*System.out.println(declaration.getRawSignature());
		System.err.println(compound.getRawSignature());

	}*/
	private IASTCompoundStatement makeCompoundStatement(String string) {
		// TODO Auto-generated method stub
		return null;
	}
	public void visit(IASTIfStatement statement) {
		IASTExpression condition = statement.getConditionExpression();

		// Check if the condition contains a method call expression
		if (containsMethodCall(condition)) {
			instrumentIfCondition(statement, condition);
		}
	}

	public int visit(IASTStatement statement) {
		try {
			System.out.println(statement.getRawSignature());
			if (statement instanceof IASTIfStatement)
				this.visit((IASTIfStatement) statement);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return PROCESS_CONTINUE;
	}
}
