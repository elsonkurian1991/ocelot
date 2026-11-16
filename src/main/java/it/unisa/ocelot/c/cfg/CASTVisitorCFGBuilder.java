package it.unisa.ocelot.c.cfg;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unisa.ocelot.c.cfg.edges.CaseEdge;
import it.unisa.ocelot.c.cfg.edges.FalseEdge;
import it.unisa.ocelot.c.cfg.edges.FlowEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.edges.TrueEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.types.CTypeDouble;
import it.unisa.ocelot.c.types.CTypeInteger;
import it.unisa.ocelot.c.types.CType;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IType;

/**
 * An {@link ASTVisitor} that adds to a {@link CFG} the subgraph 
 * for a C function defined in the AST, if present.
 */
public class CASTVisitorCFGBuilder extends ASTVisitor {
	private final IASTTranslationUnit translationUnit;
	private final CFG cfg;
	private final String functionName;
	private final List<IType> functionParametersTypes;

	// Used during the visit
	private final HashMap<String, CFGNode> labels;
	private final ArrayList<Entry<String, CFGNode>> gotos;
	private final ArrayList<CFGNode> returns;
	private final ArrayDeque<SubGraph> ioHandlers;

	/**
	 * Constructor.
	 * 
	 * @param pTranslationUnit a {@link ITranslationUnit}.
	 * @param pCfg the {@link CFG} to modify. It must be 
	 *        generated from {@code pTranslationUnit}.
	 * @param pFunctionName a {@link String}, the
	 *        name of a function.
	 * @param pFunctionParametersTypes a {@link List}{@code <}{@link IType}{@code >} 
	 *        with the types of {@code pFunctionName}'s parameters.
	 */
	public CASTVisitorCFGBuilder(IASTTranslationUnit pTranslationUnit, CFG pCfg, String pFunctionName, List<IType> pFunctionParametersTypes) {
		this.shouldVisitDeclarations = true;
		this.shouldVisitStatements = true;
		this.shouldVisitTranslationUnit = true;

		this.translationUnit = pTranslationUnit;
		this.cfg = pCfg;
		this.functionName = pFunctionName;
		this.functionParametersTypes = pFunctionParametersTypes;

		this.labels = new HashMap<>();
		this.gotos = new ArrayList<>();
		this.returns = new ArrayList<>();
		this.ioHandlers = new ArrayDeque<>();
	}

	public void calculate() {
		this.translationUnit.accept(this);
	}

	@Override
	public int visit(IASTTranslationUnit tu) {
		CType[] parameterTypes = new CType[this.functionParametersTypes.size()];
		for (int i = 0; i < this.functionParametersTypes.size(); i++) {
			String typeName = this.functionParametersTypes.get(i).toString();
			if (typeName.startsWith("double") || typeName.startsWith("float")) {
				parameterTypes[i] = CTypeDouble.type(typeName.contains("*"));
			} else { //TODO typeName it is assumed to be an integer type name: are we sure?
				parameterTypes[i] = CTypeInteger.type(typeName.contains("*"));
			}
		}
		this.cfg.setParameterTypes(parameterTypes);

		return super.visit(tu);
	}

	@Override
	public int visit(IASTDeclaration name) {
		if (name instanceof IASTFunctionDefinition) {
			IASTFunctionDefinition function = (IASTFunctionDefinition) name;

			if (!function.getDeclarator().getName().getRawSignature()
					.equals(this.functionName)) {
				return PROCESS_SKIP;
			}

			CFGNode.reset();
			CFGNode vertexEntry = new CFGNode("Start");
			this.cfg.addVertexEntry(vertexEntry);

			function.getBody().accept(this);
			SubGraph body = this.ioHandlers.pop();

			CFGNode vertexExit = new CFGNode("End");
			this.cfg.addVertexExit(vertexExit);

			setOutput(vertexEntry, body.getInputs(), FlowEdge.class);
			addEdges(body.getOutputs(), vertexExit, FlowEdge.class);
			addEdges(body.getBreaks(), vertexExit, FlowEdge.class);

			onExit();
		}

		return PROCESS_SKIP;
	}

	/**
	 * Invoked to complete the CFG from the 
	 * IASTFunctionDefinition of the searched
	 * function.
	 */
	private void onExit() {
		//adds all the gotos to the cfg
		for (Entry<String, CFGNode> entry : this.gotos) {
			addEdge(entry.getValue(), this.labels.get(entry.getKey()), FlowEdge.class);
		}
		addEdges(this.returns, this.cfg.getExit(), FlowEdge.class);

		List<CFGNode> toRemove = new ArrayList<>();
		for (CFGNode node : this.cfg.vertexSet()) {
			Set<LabeledEdge> edges = this.cfg.outgoingEdgesOf(node);
			boolean hasTrueEdge = false;
			for (LabeledEdge edge : edges) {
				if (edge instanceof TrueEdge)
					hasTrueEdge = true;
			}

			if (hasTrueEdge) {
				for (LabeledEdge edge : edges) {
					if (edge instanceof FlowEdge) {
						CFGNode source = this.cfg.getEdgeSource(edge);
						CFGNode dest = this.cfg.getEdgeTarget(edge);
						this.cfg.removeEdge(edge);
						this.addEdge(source, dest, FalseEdge.class);
					}
				}
			}

			if (node.isContinue() || node.isBreak() || node.isGoto()) {
				Set<LabeledEdge> incomingEdges = 
						this.cfg.incomingEdgesOf(node);
				Set<LabeledEdge> outgoingEdges = 
						this.cfg.outgoingEdgesOf(node);

				for (LabeledEdge incoming : incomingEdges) {
					for (LabeledEdge outgoing : outgoingEdges) {
						CFGNode source = this.cfg.getEdgeSource(incoming);
						CFGNode dest = this.cfg.getEdgeTarget(outgoing);
						this.addEdge(source, dest,
								(LabeledEdge) incoming.clone());
						toRemove.add(node);
					}
				}
			}
		}

		for (CFGNode dead : toRemove) {
			this.cfg.removeVertex(dead);
		}
	}

	@Override
	public int visit(IASTStatement statement) {
		// Since the superclass does not offer
		// methods to visit the different kinds
		// of C statements, we have to manually
		// dispatch to local methods that handle
		// the different (many) cases separately.
		if (statement instanceof IASTCompoundStatement) {
			visit((IASTCompoundStatement) statement);
		} else if (statement instanceof IASTDeclarationStatement) {
			visit((IASTDeclarationStatement) statement);
		} else if (statement instanceof IASTExpressionStatement) {
			visit((IASTExpressionStatement) statement);
		} else if (statement instanceof IASTNullStatement) {
			visit((IASTNullStatement) statement);
		} else if (statement instanceof IASTIfStatement) {
			visit((IASTIfStatement) statement);
		} else if (statement instanceof IASTSwitchStatement) {
			visit((IASTSwitchStatement) statement);
		} else if (statement instanceof IASTWhileStatement) {
			visit((IASTWhileStatement) statement);
		} else if (statement instanceof IASTDoStatement) {
			visit((IASTDoStatement) statement);
		} else if (statement instanceof IASTForStatement) {
			visit((IASTForStatement) statement);
		} else if (statement instanceof IASTGotoStatement) {
			visit((IASTGotoStatement) statement);
		} else if (statement instanceof IASTBreakStatement) {
			visit((IASTBreakStatement) statement);
		} else if (statement instanceof IASTContinueStatement) {
			visit((IASTContinueStatement) statement);
		} else if (statement instanceof IASTReturnStatement) {
			visit((IASTReturnStatement) statement);
		} else if (statement instanceof IASTLabelStatement) {
			visit((IASTLabelStatement) statement);
		} else if (statement instanceof IASTCaseStatement) {
			visit((IASTCaseStatement) statement);
		} else if (statement instanceof IASTDefaultStatement) {
			visit((IASTDefaultStatement) statement);
		} else {
			throw new RuntimeException("Hey, this is a BUG! We found int the AST a statement with class "
					+ statement.getClass().toString() + " that we do not handle");
		}

		return PROCESS_SKIP;
	}
	
	//Helper method about statements

	/**
	 * Returns true if the statement is an expression, a NullStatement or a declaration
	 */
	private boolean isSimpleStatement(IASTStatement pStatement) {
		return (pStatement instanceof IASTExpressionStatement)
				|| (pStatement instanceof IASTNullStatement)
				|| (pStatement instanceof IASTDeclarationStatement);
	}

	/**
	 * Returns true if the statement is a label, a case or a default
	 * @param pStatement
	 * @return
	 */
	private boolean isStartingSimpleStatement(IASTStatement pStatement) {
		return (pStatement instanceof IASTLabelStatement)
				|| (pStatement instanceof IASTCaseStatement)
				|| (pStatement instanceof IASTDefaultStatement);
	}

	/**
	 * Returns true if the statement is a break, a return a goto or a continue
	 * @param pStatement
	 * @return
	 */
	private boolean isEndingSimpleStatement(IASTStatement pStatement) {
		return (pStatement instanceof IASTBreakStatement)
				|| (pStatement instanceof IASTReturnStatement)
				|| (pStatement instanceof IASTGotoStatement)
				|| (pStatement instanceof IASTContinueStatement);
	}
	
	//All the methods handling the different kinds of statements

	// #############################################
	// # Ordinary statements #
	// #############################################
	/**
	 * Visits a compound statement, so a block of instructions. Creates an edge
	 * from each sub-graph generated by the visit of each statement to the next
	 * one.
	 * 
	 * @param pStatement
	 *            Node visited
	 */
	public void visit(IASTCompoundStatement pStatement) {
		SubGraph subGraph = new SubGraph();
		boolean keepItSimple = false;

		if (pStatement.getStatements().length != 0) {
			pStatement.getStatements()[0].accept(this);
			SubGraph lastSubGraph = this.ioHandlers.pop();

			subGraph.inheritInput(lastSubGraph);
			subGraph.inheritOthers(lastSubGraph);

			if (this.isSimpleStatement(pStatement.getStatements()[0])
					|| this.isStartingSimpleStatement(pStatement
							.getStatements()[0]))
				keepItSimple = true;

			for (int i = 1; i < pStatement.getStatements().length; i++) {
				IASTStatement currentStatement = pStatement.getStatements()[i];
				currentStatement.accept(this);
				SubGraph currentSubGraph = this.ioHandlers.pop();

				if (this.isSimpleStatement(currentStatement)
						|| this.isEndingSimpleStatement(currentStatement)) {
					if (keepItSimple) {
						CFGNode lastVertex = lastSubGraph.getInputs().get(0);
						CFGNode newVertex = currentSubGraph.getInputs().get(0);

						lastVertex.join(newVertex);
						lastSubGraph.inheritBreak(currentSubGraph);
						lastSubGraph.inheritContinue(currentSubGraph);
						if (currentSubGraph.getOutputs().size() == 0)
							lastSubGraph.resetOutput();
						currentSubGraph = lastSubGraph;

						currentSubGraph.joinVertices(newVertex, lastVertex);
						this.joinVertices(newVertex, lastVertex);
					} else {
						keepItSimple = true;
						this.setOutput(lastSubGraph.getOutputs(),
								currentSubGraph.getInputs(), FlowEdge.class);
					}

					if (this.isEndingSimpleStatement(currentStatement))
						keepItSimple = false;
				} else {
					keepItSimple = false;
					if (currentSubGraph.getInputs().get(0).isCase()) {
						this.setOutput(lastSubGraph.getOutputs(),
								currentSubGraph.getInputs(), FlowEdge.class);
						//Here there was a bug once. No more. Maybe...
					} else
						this.setOutput(lastSubGraph.getOutputs(),
								currentSubGraph.getInputs(), FlowEdge.class);
				}

				if (this.isStartingSimpleStatement(currentStatement))
					keepItSimple = true;

				subGraph.inheritOthers(currentSubGraph);

				lastSubGraph = currentSubGraph;
			}

			subGraph.inheritOutput(lastSubGraph);
		} else {
			CFGNode voidBlock = new CFGNode(pStatement);
			this.cfg.addVertex(voidBlock);
			subGraph.addInput(voidBlock);
			subGraph.addOutput(voidBlock);
		}

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a declaration. Creates a vertex in the graph.
	 * 
	 * @param pStatement Node visited
	 */
	public void visit(IASTDeclarationStatement pStatement) {
		SubGraph subGraph = new SubGraph();
		CFGNode thisNode = new CFGNode(pStatement);
		this.cfg.addVertex(thisNode);
		subGraph.addInput(thisNode);
		subGraph.addOutput(thisNode);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a single statement. Creates a vertex in the graph.
	 * 
	 * @param pStatement
	 *            Node visited
	 */
	public void visit(IASTExpressionStatement pStatement) {
		SubGraph subGraph = new SubGraph();
		CFGNode thisNode = new CFGNode(pStatement);
		this.cfg.addVertex(thisNode);
		subGraph.addInput(thisNode);
		subGraph.addOutput(thisNode);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a null statement (";"). Creates a vertex in the graph.
	 * 
	 * @param pStatement
	 *            Node visited
	 */
	public void visit(IASTNullStatement pStatement) {
		SubGraph subGraph = new SubGraph();
		CFGNode thisNode = new CFGNode(pStatement);
		this.cfg.addVertex(thisNode);
		subGraph.addInput(thisNode);
		subGraph.addOutput(thisNode);

		this.ioHandlers.push(subGraph);
	}

	// #############################################
	// # Conditional statements #
	// #############################################
	/**
	 * Visits an "if" statement. Visits both then and else clauses, linking the
	 * vertexes in the graph with true and false edges.
	 * 
	 * @param pStatement the {@link IASTIfStatement} visited.
	 */
	public void visit(IASTIfStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		boolean elsePresent = pStatement.getElseClause() != null;

		CFGNode condition = new CFGNode(pStatement.getConditionExpression());
		this.cfg.addVertex(condition);

		pStatement.getThenClause().accept(this);
		SubGraph thenSubGraph = this.ioHandlers.pop();
		subGraph.inheritOutput(thenSubGraph);
		subGraph.inheritOthers(thenSubGraph);
		setOutput(condition, thenSubGraph.getInputs(), TrueEdge.class);

		if (elsePresent) {
			pStatement.getElseClause().accept(this);
			SubGraph elseSubGraph = this.ioHandlers.pop();
			setOutput(condition, elseSubGraph.getInputs(), FalseEdge.class);
			subGraph.inheritOutput(elseSubGraph);
			subGraph.inheritOthers(elseSubGraph);
		} else {
			subGraph.addOutput(condition);
		}

		subGraph.addInput(condition);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a switch statement. Links the expression to each "case"
	 * instruction in the statement. Each edge has the expression of the "case"
	 * statement as a label. Links "break"s to the next statement.
	 * 
	 * @param pStatement the {@link IASTSwitchStatement} visited.
	 */
	public void visit(IASTSwitchStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode expression = new CFGNode(pStatement.getControllerExpression());
		expression.setSwitch();
		this.cfg.addVertex(expression);

		pStatement.getBody().accept(this);
		SubGraph statementSubGraph = this.ioHandlers.pop();

		boolean defaultTaken = false;
		for (CFGNode caseNode : statementSubGraph.getCases()) {
			String label;
			if (caseNode.getLeadingNode() instanceof IASTCaseStatement) {
				IASTCaseStatement caseStatement = (IASTCaseStatement) caseNode
						.getLeadingNode();
				label = caseStatement.getExpression().getRawSignature();
			} else {
				label = "default";
				defaultTaken = true;
			}

			addEdge(expression, caseNode, new CaseEdge(label));
		}

		//If there is no default in switch...
		if (!defaultTaken) {
			CFGNode voidDefaultNode = new CFGNode("Noop");
			this.cfg.addVertex(voidDefaultNode);
			addEdge(expression, voidDefaultNode, new CaseEdge("default"));
			subGraph.addOutput(voidDefaultNode);
		}

		subGraph.addInput(expression);
		subGraph.addOutput(statementSubGraph.getOutputs());
		subGraph.addOutput(statementSubGraph.getBreaks());
		subGraph.inheritContinue(statementSubGraph);

		this.ioHandlers.push(subGraph);
	}

	// #############################################
	// # Loop statements #
	// #############################################
	/**
	 * Visits a while loop. Links the condition to both the statement and the
	 * next node; links also the statement back to the condition. Links "break"s
	 * to the next vertex and "continue"s to the condition.
	 * 
	 * @param pStatement the {@link IASTWhileStatement} visited.
	 */
	public void visit(IASTWhileStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode condition = new CFGNode(pStatement.getCondition());
		this.cfg.addVertex(condition);

		pStatement.getBody().accept(this);
		SubGraph bodySubGraph = this.ioHandlers.pop();

		setOutput(condition, bodySubGraph.getInputs(), TrueEdge.class);
		addEdges(bodySubGraph.getOutputs(), condition, FlowEdge.class);
		addEdges(bodySubGraph.getContinues(), condition, FlowEdge.class);

		subGraph.addInput(condition);
		subGraph.addOutput(condition);
		subGraph.addOutput(bodySubGraph.getBreaks());
		subGraph.inheritCase(bodySubGraph);

		this.ioHandlers.push(subGraph);
	}

	//TODO comments
	public void visit(IASTDoStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode condition = new CFGNode(pStatement.getCondition());
		this.cfg.addVertex(condition);

		pStatement.getBody().accept(this);
		SubGraph bodySubGraph = this.ioHandlers.pop();

		setOutput(condition, bodySubGraph.getInputs(), TrueEdge.class);
		addEdges(bodySubGraph.getOutputs(), condition, FlowEdge.class);
		addEdges(bodySubGraph.getContinues(), condition, FlowEdge.class);

		subGraph.inheritInput(bodySubGraph);
		subGraph.addOutput(condition);
		subGraph.addOutput(bodySubGraph.getBreaks());
		subGraph.inheritCase(bodySubGraph);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a for statement. Creates a vertex for each part of the declaration
	 * (initialization, condition and iteration) and links all the parts,
	 * including the body. Links also the "break"s to the next vertex and the
	 * "continue"s to the iteration expression.
	 * 
	 * @param pStatement the {@link IASTForStatement} visited.
	 */
	public void visit(IASTForStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode condition = new CFGNode(pStatement.getConditionExpression());
		CFGNode incr = new CFGNode(pStatement.getIterationExpression());
		this.cfg.addVertex(condition);
		this.cfg.addVertex(incr);

		pStatement.getInitializerStatement().accept(this);
		SubGraph initSubGraph = this.ioHandlers.pop();
		pStatement.getBody().accept(this);
		SubGraph bodySubGraph = this.ioHandlers.pop();

		addEdges(initSubGraph.getOutputs(), condition, FlowEdge.class);
		setOutput(condition, bodySubGraph.getInputs(), TrueEdge.class);
		addEdges(bodySubGraph.getOutputs(), incr, FlowEdge.class);
		addEdge(incr, condition, FlowEdge.class);
		addEdges(bodySubGraph.getContinues(), incr, FlowEdge.class);

		subGraph.inheritInput(initSubGraph);
		subGraph.inheritCase(bodySubGraph);
		subGraph.addOutput(condition);
		subGraph.addOutput(bodySubGraph.getBreaks());

		this.ioHandlers.push(subGraph);
	}

	// #############################################
	// # Jump statements #
	// #############################################
	/**
	 * Visits a goto statement. Adds the statement to the list of gotos to be
	 * linked.
	 * 
	 * @param pStatement the {@link IASTGotoStatement} visited.
	 */
	public void visit(IASTGotoStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		this.cfg.addVertex(node);
		String label = pStatement.getName().getRawSignature();

		this.gotos.add(new AbstractMap.SimpleEntry<String, CFGNode>(label, node));

		subGraph.addInput(node);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a break statement. Will be linked by a father to another vertex
	 * 
	 * @param pStatement the {@link IASTBreakStatement} visited.
	 */
	public void visit(IASTBreakStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		this.cfg.addVertex(node);

		subGraph.addInput(node);
		subGraph.addBreak(node);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a continue statement. Will be linked by a father to another
	 * vertex.
	 * 
	 * @param pStatement the {@link IASTContinueStatement} visited.
	 */
	public void visit(IASTContinueStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		this.cfg.addVertex(node);

		subGraph.addInput(node);
		subGraph.addContinue(node);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a return statement. Will be linked when the visit finishes to the
	 * "end" node.
	 * 
	 * @param pStatement the {@link IASTReturnStatement} visited.
	 */
	public void visit(IASTReturnStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		this.cfg.addVertex(node);

		subGraph.addInput(node);

		this.returns.add(node);

		this.ioHandlers.push(subGraph);
	}

	// #############################################
	// # Label statements #
	// #############################################
	/**
	 * Visits a label statements. Adds it to the list of labels that will be
	 * linked when the visit finishes.
	 * 
	 * @param pStatement the {@link IASTLabelStatement} visited.
	 */
	public void visit(IASTLabelStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		cfg.addVertex(node);
		this.labels.put(pStatement.getName().getRawSignature(), node);

		subGraph.addInput(node);
		subGraph.addOutput(node);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a case statement that will be linked when the father switch
	 * statement will be visited.
	 * 
	 * @param pStatement the {@link IASTCaseStatement} visited.
	 */
	public void visit(IASTCaseStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		cfg.addVertex(node);

		subGraph.addInput(node);
		subGraph.addOutput(node);
		subGraph.addCase(node);

		this.ioHandlers.push(subGraph);
	}

	/**
	 * Visits a default statement that will be linked when the father switch
	 * statement will be visited.
	 * 
	 * @param pStatement the {@link IASTDefaultStatement} visited.
	 */
	public void visit(IASTDefaultStatement pStatement) {
		SubGraph subGraph = new SubGraph();

		CFGNode node = new CFGNode(pStatement);
		cfg.addVertex(node);

		subGraph.addInput(node);
		subGraph.addOutput(node);
		subGraph.addCase(node);

		this.ioHandlers.push(subGraph);
	}
	
	//Helper methods to manipulate the CFG

	private void joinVertices(CFGNode pToRemove, CFGNode pNewNode) {
		if (this.returns.contains(pToRemove)) {
			this.returns.remove(pToRemove);
			this.returns.add(pNewNode);
		}

		for (Entry<String, CFGNode> node : this.gotos) {
			if (node.getValue().equals(pToRemove)) {
				node.setValue(pNewNode);
			}
		}

		this.cfg.removeVertex(pToRemove);
	}

	/**
	 * Adds a set of edges to {@link #cfg}.
	 * 
	 * @param pOut a {@link List}{@code <}{@link CFGNode}{@code >}. 
	 *        The edges will start from here.
	 * @param pIn a {@link List}{@code <}{@link CFGNode}{@code >}. 
	 *        The edges will end here.
	 * @param pEdgeClass a {@link Class}{<? extends }{@link LabeledEdge}{@code >}, 
	 *        the class of the edge to be built.
	 */
	private void setOutput(List<CFGNode> pOut, List<CFGNode> pIn, Class<? extends LabeledEdge> pEdgeClass) {
		for (CFGNode out : pOut) {
			for (CFGNode in : pIn) {
				addEdge(out, in, pEdgeClass);
			}
		}
	}

	/**
	 * Adds a set of edges to {@link #cfg}.
	 * 
	 * @param pOut a {@link CFGNode}. The edges will start from here.
	 * @param pIn a {@link List}{@code <}{@link CFGNode}{@code >}. 
	 *             The edges will end here.
	 * @param pEdgeClass a {@link Class}{<? extends }{@link LabeledEdge}{@code >}, 
	 *        the class of the edge to be built.
	 */
	private void setOutput(CFGNode pOut, List<CFGNode> pIn, Class<? extends LabeledEdge> pEdgeClass) {
		for (CFGNode in : pIn) {
			addEdge(pOut, in, pEdgeClass);
		}
	}

	/**
	 * Adds a set of edges to {@link #cfg}.
	 * 
	 * @param pOut a {@link List}{@code <}{@link CFGNode}{@code >}. 
	 *        The edges will start from here.
	 * @param pIn a {@link CFGNode}. The edges will end here.
	 * @param pEdgeClass a {@link Class}{<? extends }{@link LabeledEdge}{@code >}, 
	 *        the class of the edge to be built.
	 */
	private void addEdges(List<CFGNode> pOut, CFGNode pIn, Class<? extends LabeledEdge> pEdgeClass) {
		for (CFGNode out : pOut) {
			addEdge(out, pIn, pEdgeClass);
		}
	}

	/**
	 * Connects two nodes in {@link #cfg}.
	 * 
	 * @param pOut a {@link CFGNode}. The edge will start from here.
	 * @param pIn a {@link CFGNode}. The edge will end here.
	 * @param pEdgeClass a {@link Class}{<? extends }{@link LabeledEdge}{@code >}, 
	 *        the class of the edge to be built.
	 */
	private void addEdge(CFGNode pOut, CFGNode pIn, Class<? extends LabeledEdge> pEdgeClass) {
		try {
			Constructor<? extends LabeledEdge> edgeConstructor = pEdgeClass.getConstructor();
			LabeledEdge edge = edgeConstructor.newInstance();
			addEdge(pOut, pIn, edge);
		} catch (NoSuchMethodException | SecurityException |
				InstantiationException | IllegalAccessException | 
				IllegalArgumentException | InvocationTargetException e) {
			RuntimeException ex = new RuntimeException("Unable to reflectively make an edge with class " + pEdgeClass.getName() + ". Do you have permissions? Does the class have a no-parameter constructor?");
			ex.addSuppressed(e);
			throw ex;
		}
	}

	/**
	 * Links the given output to the given input in the CFG
	 * 
	 * @param pOut a {@link CFGNode}. The edge will start from here.
	 * @param pIn a {@link CFGNode}. The edge will end here.
	 * @param pEdge a {@link LabeledEdge}. This will used as edge between 
	 *        {@code pSource} and {@code pTarget}.
	 */
	private void addEdge(CFGNode pOut, CFGNode pIn, LabeledEdge pEdge) {
		this.cfg.addEdge(pOut, pIn, pEdge);
	}
	
	//Final public getter

	/**
	 * Returns the CFG built by this visitor.
	 * Invoke it after a visit of an
	 * AST to obtain the result.
	 * 
	 * @return a {@link CFG}.
	 */
	public CFG getCFG() {
		return this.cfg;
	}
}
