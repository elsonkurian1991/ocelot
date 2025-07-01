package it.unisa.ocelot.c.instrumentor;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTIfStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTName;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTNullStatement;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGVisitor;

public class InstrumentorVisitorToAddBranch extends ASTVisitor{
	private final String functionName;
	private final ASTRewrite rewriter;

	public InstrumentorVisitorToAddBranch(String pInstrumentFunction, ASTRewrite rewriter) {
		// TODO Auto-generated constructor stub
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

		tu.accept(new CFGVisitor(new CFG(), this.functionName));

		return super.visit(tu);
	}
	@Override
    public int visit(IASTDeclaration declaration) {
        
        if (declaration instanceof IASTFunctionDefinition) {
            IASTFunctionDefinition functionDef = (IASTFunctionDefinition) declaration;
                       
            IASTStatement body = functionDef.getBody();
            
            if (body instanceof IASTCompoundStatement) {
            	String ifCond="_f_ocelot_branch_out("+this.functionName+",0,true,0,1)";
                CASTCompoundStatement compoundBody = (CASTCompoundStatement) body;
                
                CASTCompoundStatement substitute = new CASTCompoundStatement();
                for (IASTStatement stmt : compoundBody.getStatements()) {
                	 substitute.addStatement(stmt.copy()); 	
                }
                CASTLiteralExpression cond = new CASTLiteralExpression(IASTLiteralExpression.lk_integer_constant,ifCond);
                CASTIfStatement ifStmt = new CASTIfStatement();
                ifStmt.setConditionExpression(cond);
                ifStmt.setThenClause(substitute);
                
                CASTCompoundStatement newBody = new CASTCompoundStatement();
                newBody.addStatement(ifStmt);
                functionDef.setBody(newBody);
               // CASTCompoundStatement compoundBody2 = new CASTCompoundStatement();
                
                /*IASTStatement[] originalStatements = compoundBody.getStatements();
                IASTStatement firstStmt=originalStatements[0];
                
                CASTCompoundStatement substitute = new CASTCompoundStatement();
                substitute.addStatement(firstStmt.copy());
                
                CASTLiteralExpression cond = new CASTLiteralExpression(IASTLiteralExpression.lk_integer_constant,"1");
                
                CASTIfStatement ifStmt = new CASTIfStatement();
                ifStmt.setConditionExpression(cond);
                ifStmt.setThenClause(substitute);
                compoundBody.replace(firstStmt, ifStmt);
                
              /*  IASTFunctionCallExpression call = new CASTFunctionCallExpression();
        		IASTIdExpression name = new CASTIdExpression(new CASTName("this_fun_call".toCharArray()));

        		call.setFunctionNameExpression(name);
        		call.setArguments(new IASTInitializerClause[0]);
                
                compoundBody2.addStatement(new CASTExpressionStatement(call));
                
                
                
                for(int i=0; i<originalStatements.length; i++) {
                	compoundBody2.addStatement(originalStatements[i]);
                	if (i == 6)
                		break;
                }
                compoundBody.replace(originalStatements[0], compoundBody2);
                /*for(int i=1; i<originalStatements.length; i++) {
                	compoundBody.replace(originalStatements[i], new CASTNullStatement());
                }*/
                
                
				/*for( IASTStatement is : originalStatements) {
                	compoundBody.replace(is, new CASTNullStatement());
                }*/
                
                
                
                
               
               
                

                
                /*
                
                // Get the AST from translation unit
                IASTTranslationUnit tu = functionDef.getTranslationUnit();
                
                // Create new compound statement using AST factory
                CASTCompoundStatement substitute = new CASTCompoundStatement();
                functionDef.getBody().accept(this);
                
        		

                
                // Create function call: this_fun_call();
                /*IASTName callName = tu.getASTNodeFactory().newName("this_fun_call".toCharArray());
                IASTIdExpression idExpr = tu.getASTNodeFactory().newIdExpression(callName);
                IASTInitializerClause[] arguments = new IASTInitializerClause[0]; // Empty arguments array
                IASTFunctionCallExpression functionCall = tu.getASTNodeFactory().newFunctionCallExpression(idExpr, arguments);
                IASTExpressionStatement callStmt = tu.getASTNodeFactory().newExpressionStatement(functionCall);
                
                System.out.printf("DEBUG: Created function call statement of type: %s%n", callStmt.getClass().getSimpleName());
                System.out.printf("DEBUG: Function call expression: %s%n", functionCall.getClass().getSimpleName());
                System.out.printf("DEBUG: Function name in call: %s%n", new String(callName.toCharArray()));*/
                // Add the function call first
                
                
                // Add all existing statements
                /*IASTStatement[] originalStatements = compoundBody.getStatements();

                
                substitute.addStatement(new CASTExpressionStatement(call));
                substitute.addStatement(originalStatements[0]);
                
                //originalStatements[0] = substitute;
                
                /*for (int i = 0; i < originalStatements.length; ++i) {
        			if (originalStatements[i] == originalStatements[0]) {
        				substitute.setParent(originalStatements[i].getParent());
        				substitute.setPropertyInParent(originalStatements[i].getPropertyInParent());
        				originalStatements[i] = (IASTStatement) substitute;
        				break;
        			}
        		}*/
                
                /*compoundBody.replace(originalStatements[0], substitute);
                System.out.println(compoundBody.getRawSignature());
                
                System.out.printf("DEBUG: Substitute compound statement now has %d statements%n", 
                    substitute.getStatements().length);*/
                

            } 
        }
        
        return PROCESS_CONTINUE;
    }
	
	/*public int visit(IASTDeclaration declaration) {
		try {
			if (declaration instanceof IASTFunctionDefinition) {
				
				 IASTFunctionDefinition funcDef = (IASTFunctionDefinition) declaration;
				 IASTStatement body = funcDef.getBody();
				 //System.out.println(body.getRawSignature());
				 if (body instanceof IASTCompoundStatement) {
					 IASTCompoundStatement compound = (IASTCompoundStatement) body;
					// System.err.println(compound.getRawSignature());
					// Create the new compound statement using the AST factory
					 // Get the AST from translation unit
					   // Get the AST from translation unit
		                IASTTranslationUnit tu = funcDef.getTranslationUnit();
		                
		                // Create new compound statement using AST factory
		                IASTCompoundStatement substitute = tu.getASTNodeFactory().newCompoundStatement();
		                
		                // Create function call: this_fun_call();
		                IASTName functionName = tu.getASTNodeFactory().newName("this_fun_call".toCharArray());
		                IASTIdExpression idExpr = tu.getASTNodeFactory().newIdExpression(functionName);
		                IASTInitializerClause[] arguments = new IASTInitializerClause[0]; // Empty arguments array
		                IASTFunctionCallExpression functionCall = tu.getASTNodeFactory().newFunctionCallExpression(idExpr, arguments);
		                IASTExpressionStatement callStmt = tu.getASTNodeFactory().newExpressionStatement(functionCall);
		                
		                // Add the function call first
		                substitute.addStatement(callStmt);
		                
		                // Add all existing statements
		                IASTStatement[] originalStatements = compound.getStatements();
		                for (IASTStatement stmt : originalStatements) {
		                    substitute.addStatement(stmt);
		                }
		                
		                // Replace using the rewriter
		                rewriter.replace(compound, substitute, null);
					/* CASTIdExpression branch_out_Id = new CASTIdExpression();
					 branch_out_Id.setName(new CASTName("this_is_fun".toCharArray()));
					 

		                CASTFunctionCallExpression callExpr = new CASTFunctionCallExpression();
		                callExpr.setFunctionNameExpression(branch_out_Id);
		              

		               // callExpr.addArgument(stringLiteral);

		                CASTExpressionStatement callStmt = new CASTExpressionStatement();
		               callStmt.setExpression(callExpr);
		                System.err.println(compound.getRawSignature());
		                // Insert printf at start of compound statement
		                // Set parent relationships
		                
		                
		                // Add the function call as first statement
		                substitute.addStatement(callStmt);
		                substitute.addStatement(callStmt);
		                for (IASTStatement stmt : compound.getStatements()) {
		                	substitute.addStatement(stmt);
		                }
		                rewriter.replace(compound, substitute, null);
		                /*IASTStatement[] statements = compound.getStatements();
		               
		                if (statements.length > 0) {
		                    rewriter.insertBefore(compound, statements[0], callStmt, null);
		                } else {
		                    rewriter.insertBefore(compound, null, callStmt, null);
		                }
		                System.out.println(compound.getRawSignature());
				 }
			}
			//System.out.println(declaration.getRawSignature());
				//instrumentFunctionFirstLine((IASTFunctionDefinition) declaration);

			
		}catch (Exception e) {
			e.printStackTrace();
		}

		return PROCESS_CONTINUE;		

	}*/
	/*private void instrumentFunctionFirstLine(IASTFunctionDefinition declaration) {
		// TODO Auto-generated method stub
		try {
			// Get the function declarator to check the function name
			IASTFunctionDeclarator declarator = functionDef.getDeclarator();
			String currentFunctionName = declarator.getName().toString();

			// Only instrument if this is the target function (or instrument all functions if functionName is null/empty)
			if (this.functionName == null || this.functionName.isEmpty() || 
					currentFunctionName.equals(this.functionName)) {

				// Get the function body (compound statement)
				IASTStatement body = functionDef.getBody();
				if (body instanceof IASTCompoundStatement) {
					IASTCompoundStatement compoundBody = (IASTCompoundStatement) body;

					// Create the instrumentation string - you can customize this message
					String instrumentationCode = String.format(
							"printf(\"[INSTRUMENTATION] Entering function: %s\\n\");", 
							currentFunctionName
							);

					// Create a new expression statement from the instrumentation code
					IASTTranslationUnit tu = functionDef.getTranslationUnit();
					IASTExpressionStatement instrumentStmt = 
							tu.getASTNodeFactory().newExpressionStatement(
									tu.getASTNodeFactory().newIdExpression(
											tu.getASTNodeFactory().newName(instrumentationCode.toCharArray())
											)
									);

					// Alternative approach: Create the statement more explicitly
					// This creates a proper function call expression
					IASTName printfName = tu.getASTNodeFactory().newName("printf".toCharArray());
					IASTIdExpression printfId = tu.getASTNodeFactory().newIdExpression(printfName);

					String message = String.format("[INSTRUMENTATION] Entering function: %s\\n", currentFunctionName);
					IASTLiteralExpression stringLiteral = tu.getASTNodeFactory().newLiteralExpression(
							IASTLiteralExpression.lk_string_literal, 
							("\"" + message + "\"").toCharArray()
							);

					IASTExpressionList argList = tu.getASTNodeFactory().newExpressionList();
					argList.addExpression(stringLiteral);

					IASTFunctionCallExpression printfCall = tu.getASTNodeFactory().newFunctionCallExpression(
							printfId, argList
							);

					IASTExpressionStatement printfStmt = tu.getASTNodeFactory().newExpressionStatement(printfCall);

					// Get existing statements
					IASTStatement[] existingStatements = compoundBody.getStatements();

					// Create new array with instrumentation as first statement
					IASTStatement[] newStatements = new IASTStatement[existingStatements.length + 1];
					newStatements[0] = printfStmt;
					System.arraycopy(existingStatements, 0, newStatements, 1, existingStatements.length);

					// Create new compound statement with instrumentation
					IASTCompoundStatement newBody = tu.getASTNodeFactory().newCompoundStatement(newStatements);

					// Use the rewriter to replace the old body with the new one
					this.rewriter.replace(body, newBody, null);

					System.out.println("Instrumented function: " + currentFunctionName);
				}
			}
		} catch (Exception e) {
			System.err.println("Error instrumenting function: " + e.getMessage());
			e.printStackTrace();
		}
	}*/

}
