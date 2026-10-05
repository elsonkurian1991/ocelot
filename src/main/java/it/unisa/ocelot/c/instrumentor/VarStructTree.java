package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.internal.core.dom.parser.c.CPointerType;
import org.eclipse.cdt.internal.core.dom.parser.c.CStructure;
import org.eclipse.cdt.internal.core.dom.parser.c.CTypedef;

public class VarStructTree {
	private StructNode root;
	
	public VarStructTree(String pVarName, CStructure pStructure) {
		this.root = new StructNode(pVarName, pStructure);
		this.root.parent = null;
	}
	
	public StructNode getRoot() {
		return root;
	}
	
	public List<StructNode> getBasicVariables() {
		List<StructNode> leaves = new ArrayList<>();
		
		Queue<StructNode> queue = new LinkedList<>();
		queue.add(this.root);		
		while (!queue.isEmpty()) {
			StructNode node = queue.poll();
			
			if (node.isLeaf()) {
				leaves.add(node);
			} else {
				queue.addAll(node.children);
			}
		}
		
		return leaves;
	}
}


class StructNode {
	public IType type;
	public String name;
	public List<StructNode> children;
	public StructNode parent;
	public boolean pointer;
	
	public StructNode(String pName, IType pType) {
		this.type = this.resolveType(pType);
		this.name = pName;
		this.build();
		this.pointer = false;
	}
	
	private void build() {
		if (this.type instanceof CStructure) {
			CStructure struct = (CStructure) this.type;
			IField[] fields = struct.getFields();
			
			this.children = new ArrayList<>();
			for (IField field : fields) {
				StructNode node = new StructNode(field.getName(), field.getType());
				node.parent = this;
				this.children.add(node);
			}
		} else {
			this.children = null; //not a struct
		}
	}
	
	private IType resolveType(IType pType) {
		IType retVal = pType;
		while (retVal instanceof CTypedef) {
			retVal = ((CTypedef) retVal).getType();
		}
		return retVal;
	}
	
	public boolean isLeaf() {
		return this.children == null;
	}
	
	public boolean isPointer() {
		return this.type instanceof CPointerType || this.pointer;
	}
	
	public String getCompleteNameWithPointers() {
		return (this.parent == null ? "" : (this.parent.getCompleteName() + (this.parent.isPointer() ? "->" : "."))) + this.name;
	}
	
	public String getCompleteName() {
		return (this.parent == null ? "" : (this.parent.getCompleteName() + ".")) + this.name;
	}
}