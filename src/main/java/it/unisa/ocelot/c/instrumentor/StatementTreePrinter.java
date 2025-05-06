//import org.eclipse.cdt.core.dom.ast.*;
package it.unisa.ocelot.c.instrumentor;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;


// Used to print the structure of the AST for debugging
public class StatementTreePrinter extends ASTVisitor {

    private int indent = 0;

    public StatementTreePrinter() {
        super();
        shouldVisitStatements = true;
        shouldVisitExpressions = true;
    }
    


    @Override
    public int visit(IASTStatement stmt) {
        printIndented(stmt.getClass().getSimpleName());

        if (stmt instanceof IASTCompoundStatement) {
            indent += 2;
            // Inutile
            // printIndented(((IASTCompoundStatement) stmt).NESTED_STATEMENT.getName());
            for (IASTStatement child : ((IASTCompoundStatement) stmt).getStatements()) {
                child.accept(this);
            }
            indent -= 2;
        } else if (stmt instanceof IASTIfStatement) {
            IASTIfStatement ifStmt = (IASTIfStatement) stmt;

            indent += 2;
            printIndented("Condition: " + ifStmt.getConditionExpression().getRawSignature());

            printIndented("Then:");
            ifStmt.getThenClause().accept(this);

            if (ifStmt.getElseClause() != null) {
                printIndented("Else:");
                ifStmt.getElseClause().accept(this);
            }
            indent -= 2;
        } else if (stmt instanceof IASTExpressionStatement) {
            for (IASTNode child : (stmt.getChildren())) {
            	child = (IASTExpression) child;
                indent += 2;
                printIndented("12Code: " + stmt.getRawSignature().replaceAll("\\s+", " "));
                indent -= 2;
                child.accept(this);
            }
        } else {
            indent += 2;
            printIndented("Code: " + stmt.getRawSignature().replaceAll("\\s+", " "));
            indent -= 2;
        }

        return ASTVisitor.PROCESS_SKIP;
    }
    
    @Override
    public int visit(IASTExpression stmt) {
    	printIndented("Expression statement Code: " + stmt.getRawSignature().replaceAll("\\s+", " "));
    	printIndented("Expression statement Code: " + stmt.getParent().getParent().getRawSignature().replaceAll("\\s+", " "));
    	return ASTVisitor.PROCESS_SKIP;
    }
    


    private void printIndented(String message) {
        System.out.printf("%s%s%n", " ".repeat(indent), message);
    }
}
