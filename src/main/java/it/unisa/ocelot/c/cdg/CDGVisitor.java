package it.unisa.ocelot.c.cdg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.SubGraph;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

public class CDGVisitor  extends ASTVisitor  {
	private CFG graph;
	private Map<String, CFGNode> labels;
	private List<Entry<String, CFGNode>> gotos;
	private List<CFGNode> returns;
	private Stack<SubGraph> ioHandlers;
	private String functionName;
	public CDGVisitor(CFG pGraph, String pFunctionName) {
		this.graph = pGraph;
		this.functionName = pFunctionName;

		this.labels = new HashMap<String, CFGNode>();
		this.gotos = new ArrayList<Entry<String, CFGNode>>();
		this.returns = new ArrayList<CFGNode>();
		this.ioHandlers = new Stack<SubGraph>();
		
		this.shouldVisitDeclarations = true;
		this.shouldVisitStatements = true;
		this.shouldVisitTranslationUnit = true;
	}

	

}
