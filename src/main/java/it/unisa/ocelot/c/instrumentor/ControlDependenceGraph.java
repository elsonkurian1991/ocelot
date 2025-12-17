package it.unisa.ocelot.c.instrumentor;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.*;
import java.io.*;
import java.util.*;
/**
 * ControlDependenceGraph:
 *  - Visits AST nodes in a Translation Unit
 *  - Detects control structures (if, while, for, switch)
 *  - Builds a control-dependence graph
 *  - Writes the graph to a text file
 *  - .dot files for visualization
 */
public class ControlDependenceGraph extends ASTVisitor{
	private final IASTTranslationUnit translationUnit;
	private final List<BranchNode> nodes = new ArrayList<>();
	private final Stack<BranchNode> branchStack = new Stack<>();
	private int branchCounter = 0;
	private BranchNode lastTopLevelBranch = null;
	private static final File outputFile = new File("cdg_output.txt");

	public ControlDependenceGraph(IASTTranslationUnit translationUnit) {
		this.translationUnit = translationUnit;
		this.shouldVisitStatements = true;  // We care about control-flow statements
	}
	// Core visiting logic
	@Override
	public int visit(IASTStatement stmt) {
		try {
			if (stmt instanceof IASTIfStatement) {
				handleIfStatement((IASTIfStatement) stmt);
				/* } else if (stmt instanceof IASTWhileStatement) {
                handleLoop("while", ((IASTWhileStatement) stmt).getConditionExpression());*/
			} else if (stmt instanceof IASTForStatement) {
				handleLoop("for", ((IASTForStatement) stmt).getConditionExpression());
			} else if (stmt instanceof IASTDoStatement) {
				handleLoop("do-while", ((IASTDoStatement) stmt).getCondition());
			} else if (stmt instanceof IASTSwitchStatement) {
				handleLoop("switch", ((IASTSwitchStatement) stmt).getControllerExpression());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return PROCESS_CONTINUE;
	}

	private void handleIfStatement(IASTIfStatement stmt) {
		String cond = stmt.getConditionExpression() != null ? stmt.getConditionExpression().getRawSignature() : "<no condition>";
		BranchNode node = new BranchNode("Branch" + branchCounter+"-true", "if", cond);
		nodes.add(node);
		
		if (!branchStack.isEmpty()) {
			branchStack.peek().addDependent(node);
		}
		// Otherwise, link to the last top-level control (sequential dependence)
	    else if (lastTopLevelBranch != null) {
	        lastTopLevelBranch.addDependent(node);
	    }

		branchStack.push(node);

		// Handle else branch (if exists)
		if (stmt.getElseClause() != null) {
			BranchNode elseNode = new BranchNode("Branch" + branchCounter+"-false", "else", "<else>");
			nodes.add(elseNode);
			node.addDependent(elseNode);
		}
        
		// Handle "true" path (the then statement)
	   /* if (stmt.getThenClause() != null) {
	        BranchNode trueBranch = new BranchNode(node.id + "_T", "true", cond);
	        nodes.add(trueBranch);
	        node.addDependent(trueBranch);
	    }

	    // Handle "false" path (else clause)
	    if (stmt.getElseClause() != null) {
	        BranchNode falseBranch = new BranchNode(node.id + "_F", "false", cond);
	        nodes.add(falseBranch);
	        node.addDependent(falseBranch);
	    }*/
		branchCounter++;
		branchStack.pop();
		// Update the last top-level branch tracker
	    lastTopLevelBranch = node;
	}

	private void handleLoop(String type, IASTExpression condition) {
		String cond = (condition != null) ? condition.getRawSignature() : "<no condition>";
		BranchNode node = new BranchNode("Branch" + branchCounter++, type, cond);
		nodes.add(node);

		if (!branchStack.isEmpty()) {
			branchStack.peek().addDependent(node);
		}else if (lastTopLevelBranch != null) {
	        lastTopLevelBranch.addDependent(node);
	    }

	    lastTopLevelBranch = node;
	}

	/** 
	 * Call this after traversal to save the CDG to a text file.
	 * @param unitComponent 
	 */
	public void saveToFile(String unitComponent) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			writer.write("\n************************************************************************************************************************\n");
			writer.write("\n==============================\n");
			writer.write("UNIT: " + unitComponent + "\n");
			writer.write("==============================\n");
			for (BranchNode n : nodes) {
				writer.write(n.toString() + "\n");
			}
			writer.write("=== END OF GRAPH ===\n\n");
			System.out.println("CDG appended for unit: " + unitComponent);
			System.out.println("CDG successfully written to: " + outputFile.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
	}
	/**
	 * Exports the Control Dependence Graph (CDG) to a .dot file
	 * compatible with Graphviz for visualization.
	 * Each unit generates a separate .dot file.
	 * 
	 * @param unitName The name of the source/unit component (e.g., file or function name)
	 */
	public void exportToDotFile(String unitName) throws InterruptedException {
	    try {
	        final File dotOutput = new File("cdg_graph_" + unitName + ".dot");
	        final File pngOutput = new File("cdg_graph_" + unitName + ".png");

	        // Always delete old files
	        if (dotOutput.exists()) dotOutput.delete();
	        if (pngOutput.exists()) pngOutput.delete();

	        // Generate DOT graph
	        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dotOutput, false))) {
	            writer.write("digraph CDG_" + unitName.replaceAll("[^a-zA-Z0-9_]", "_") + " {\n");
	            writer.write("  rankdir=TB;\n");
	            writer.write("  node [shape=box, style=filled, color=white, fontname=\"Helvetica\"];\n");
	            writer.write("  edge [fontname=\"Helvetica\", fontsize=10];\n");
	            writer.write("  label=\"Control Dependence Graph - " + unitName + "\";\n");
	            writer.write("  labelloc=top;\n");
	            writer.write("  fontsize=18;\n\n");

	            // === Define Nodes ===
	            for (BranchNode node : nodes) {
	                String safeNodeId = node.id.replace("-", "_");
	                String label = node.id;
	                String fillColor = "#ffffff";

	                if (node.id.endsWith("true")) fillColor = "#d4fcd4";   // greenish for true
	                else if (node.id.endsWith("false")) fillColor = "#fcd4d4"; // reddish for false

	                writer.write("  \"" + safeNodeId + "\" [label=\"" + label + "\", fillcolor=\"" + fillColor + "\"];\n");
	            }

	            writer.write("\n  // === Control Dependencies ===\n");
	            for (BranchNode node : nodes) {
	                String safeNodeId = node.id.replace("-", "_");
	                for (BranchNode dep : node.dependents) {
	                    String safeDepId = dep.id.replace("-", "_");
	                    String edgeColor = "black";
	                    if (dep.id.endsWith("true")) edgeColor = "green";
	                    else if (dep.id.endsWith("false")) edgeColor = "red";
	                    writer.write("  \"" + safeNodeId + "\" -> \"" + safeDepId + "\" [color=" + edgeColor + "];\n");
	                }
	            }

	            // === Annotate Interesting Branch Pairs ===
	            List<String[]> branchPairs = getInterestingBranchPairsForDot();
	            if (!branchPairs.isEmpty()) {
	                writer.write("\n  // === Interesting Branch Pairs ===\n");
	                for (String[] pair : branchPairs) {
	                    String src = pair[0].replace("-", "_");
	                    String dst = pair[1].replace("-", "_");
	                    writer.write("  \"" + src + "\" -> \"" + dst + "\" [style=dashed, color=blue, label=\"pair\"];\n");
	                }
	            }

	            writer.write("}\n");
	        }

	        System.out.println(" CDG exported to: " + dotOutput.getAbsolutePath());

	        // === Auto-generate PNG using Graphviz ===
	        try {
	            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotOutput.getAbsolutePath(), "-o", pngOutput.getAbsolutePath());
	            pb.redirectErrorStream(true);
	            Process process = pb.start();

	            // Capture and print Graphviz output/errors
	            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	                String line;
	                while ((line = reader.readLine()) != null) {
	                    System.out.println("[graphviz] " + line);
	                }
	            }

	            int exitCode = process.waitFor();
	            if (exitCode == 0) {
	                System.out.println(" PNG generated: " + pngOutput.getAbsolutePath());
	            } else {
	                System.err.println(" Graphviz failed (exit code " + exitCode + "). Ensure 'dot' is installed and in PATH.");
	            }
	        } catch (Exception e) {
	            System.err.println(" Failed to generate PNG automatically. Ensure Graphviz is installed.");
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	/*public void exportToDotFile(String unitName) {
		
		String safeUnitName = unitName.replaceAll("\\W+", "_");
	    final File dotOutput = new File("cdg_graph_" + safeUnitName + ".dot");

	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(dotOutput, false))) {
	        writer.write("digraph CDG_" + safeUnitName + " {\n");
	        writer.write("  rankdir=TB;\n");
	        writer.write("  node [shape=box, style=filled, color=white, fontname=\"Helvetica\"];\n");
	        writer.write("  edge [fontname=\"Helvetica\", fontsize=10];\n\n");
	        writer.write("  label=\"Control Dependence Graph - " + unitName + "\";\n");
	        writer.write("  labelloc=top;\n  fontsize=18;\n\n");

	        // Define nodes
	        for (BranchNode n : nodes) {
	            String label = n.id + ": " + n.type + "\\n(" + n.condition.replace("\"", "\\\"") + ")";
	            writer.write(String.format("  %s [label=\"%s\"];\n", n.id, label));
	        }
	        writer.write("\n");

	        // Define edges with colors and labels
	        for (BranchNode n : nodes) {
	            for (BranchNode dep : n.dependents) {
	                String edgeLabel = "";
	                String color = "black";
	                String style = "solid";

	                if (dep.id.endsWith("_T")) {
	                    edgeLabel = "T";
	                    color = "green";
	                } else if (dep.id.endsWith("_F")) {
	                    edgeLabel = "F";
	                    color = "red";
	                } else if (n.id.matches("B\\d+") && dep.id.matches("B\\d+")) {
	                    // Sequential dependence between top-level ifs
	                    edgeLabel = "seq";
	                    color = "black";
	                    style = "dashed";
	                }

	                writer.write(String.format("  %s -> %s [label=\"%s\", color=%s, style=%s];\n",
	                        n.id, dep.id, edgeLabel, color, style));
	            }
	        }

	        writer.write("}\n");

	        System.out.println(" CDG DOT file generated: " + dotOutput.getAbsolutePath());

	        // --- Auto-generate PNG with Graphviz ---
	        try {
	            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng",
	                    dotOutput.getAbsolutePath(),
	                    "-o", "cdg_graph_" + safeUnitName + ".png");
	            pb.inheritIO();
	            Process p = pb.start();
	            p.waitFor();
	            System.out.println(" PNG generated: cdg_graph_" + safeUnitName + ".png");
	        } catch (Exception e) {
	            System.err.println(" Graphviz not found or failed to generate PNG: " + e.getMessage());
	        }

	    } catch (IOException e) {
	        System.err.println(" Error writing DOT file for " + unitName + ": " + e.getMessage());
	    }
		// Sanitize unit name for file naming (avoid invalid chars)
		/*String safeUnitName = unitName.replaceAll("\\W+", "_");
		final File dotOutput = new File("cdg_graph_" + safeUnitName + ".dot");
		final File pngOutput = new File("cdg_graph_" + safeUnitName + ".png");

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(dotOutput, false))) {

			// Start Graphviz digraph block
			writer.write("digraph CDG_" + safeUnitName + " {\n");
			writer.write("  rankdir=TB;\n");
			writer.write("  node [shape=box, style=filled, color=white, fontname=\"Helvetica\"];\n\n");

			// Graph title
			writer.write("  label=\"Control Dependence Graph - " + unitName + "\";\n");
			writer.write("  labelloc=top;\n  fontsize=18;\n\n");

			// --- Define Nodes with branch numbers ---
			for (BranchNode n : nodes) {
				String cleanCond = n.condition.replace("\"", "\\\"");
				String label = n.id + ": " + n.type + "\\n(" + cleanCond + ")";
				writer.write(String.format("  %s [label=\"%s\"];\n", n.id, label));
			}

			writer.write("\n");

			// --- Define Edges ---
			for (BranchNode n : nodes) {
				for (BranchNode dep : n.dependents) {
					writer.write(String.format("  %s -> %s;\n", n.id, dep.id));
				}
			}

			// End of Graphviz file
			writer.write("}\n");

			System.out.println(" CDG DOT file generated: " + dotOutput.getAbsolutePath());

		} catch (IOException e) {
			System.err.println(" Error writing DOT file for " + unitName + ": " + e.getMessage());
			return;
		}

		// --- Automatically Generate PNG using Graphviz ---
		try {
			ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng",
					dotOutput.getAbsolutePath(), "-o", pngOutput.getAbsolutePath());
			pb.redirectErrorStream(true);

			Process process = pb.start();

			// Read any Graphviz console output
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println("[Graphviz] " + line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println(" PNG generated: " + pngOutput.getAbsolutePath());
			} else {
				System.err.println(" Graphviz exited with code " + exitCode);
			}

		} catch (IOException | InterruptedException e) {
			System.err.println(" Error running Graphviz for " + unitName + ": " + e.getMessage());
		}

*/
		/*final File dotOutput = new File("cdg_graph_"+unitName+".dot");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dotOutput))) {
        	// Write digraph header if file is empty
            if (dotOutput.length() == 0) {
                writer.write("digraph CDG {\n");
                writer.write("  rankdir=TB;\n");
                writer.write("  node [shape=box, style=filled, color=white, fontname=\"Helvetica\"];\n\n");
            }
            writer.write("subgraph cluster_" + unitName.replaceAll("\\W+", "_") + " {\n");
            writer.write("  label=\"Unit: " + unitName + "\";\n");
            writer.write("  style=filled;\n  color=lightgrey;\n  node [style=filled,color=white];\n");

            // Define nodes
            for (BranchNode n : nodes) {
                writer.write(String.format("  %s [label=\"%s\\n(%s)\", shape=box];\n",
                        n.id, n.type, n.condition.replace("\"", "\\\"")));
            }

            // Define edges
            for (BranchNode n : nodes) {
                for (BranchNode dep : n.dependents) {
                    writer.write(String.format("  %s -> %s;\n", n.id, dep.id));
                }
            }

            writer.write("}\n\n");
            System.out.println("CDG DOT graph exported for unit: " + unitName);
        } catch (IOException e) {
            System.err.println("Error writing DOT file: " + e.getMessage());
        }*/
	//}
	
	/**
	 * Find and print all "interesting" control paths (branch sequences)
	 * Each path represents a sequential chain of control-dependent branches.
	 */
	public void generateInterestingBranchPaths() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			writer.write("=== Interesting Branch Paths ===");

	    // Identify root nodes (those not dependents of any others)
	    Set<BranchNode> allDependents = new HashSet<>();
	    for (BranchNode n : nodes) {
	        allDependents.addAll(n.dependents);
	    }

	    List<BranchNode> roots = new ArrayList<>();
	    for (BranchNode n : nodes) {
	        if (!allDependents.contains(n) && n.id.matches("(Branch\\d+)(-true)?")) {
	            roots.add(n);
	        }
	    }

	    // Explore each root recursively
	    for (BranchNode root : roots) {
	        List<List<BranchNode>> paths = new ArrayList<>();
	        explorePaths(root, new ArrayList<>(), paths);

	        int count = 1;
	        for (List<BranchNode> path : paths) {
	        	writer.write("\n"+count++ + ") ");
	            for (int i = 0; i < path.size(); i++) {
	                BranchNode b = path.get(i);
	                writer.write(b.id);
	                //if (b.id.endsWith("-true")) writer.write(":true");
	                //else if (b.id.endsWith("-false")) writer.write(":false");
	                if (i < path.size() - 1)writer.write(" -> ");
	            }
	            //writer.write("\n");
	        }
	    }
	    writer.write("\n===============================\n");
		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
	}

	/**
	 * Recursive helper to explore all paths.
	 */
	private void explorePaths(BranchNode current, List<BranchNode> currentPath, List<List<BranchNode>> allPaths) {
	    currentPath.add(current);

	    if (current.dependents.isEmpty()) {
	        allPaths.add(new ArrayList<>(currentPath));
	    } else {
	        for (BranchNode dep : current.dependents) {
	            explorePaths(dep, new ArrayList<>(currentPath), allPaths);
	        }
	    }
	}

	/**
	 * Generate and print interesting branch pairs
	 * (first node -> last node) from all interesting paths.
	 */
	public void generateInterestingBranchPairs() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			writer.write("\n=== Interesting Branch Pairs ===\n");

	    // Identify roots (like in generateInterestingBranchPaths)
	    Set<BranchNode> allDependents = new HashSet<>();
	    for (BranchNode n : nodes) {
	        allDependents.addAll(n.dependents);
	    }

	    List<BranchNode> roots = new ArrayList<>();
	    for (BranchNode n : nodes) {
	        if (!allDependents.contains(n) && n.id.matches("(Branch\\d+)(-true)?")) {
	            roots.add(n);
	        }
	    }

	    int pairCount = 1;

	    for (BranchNode root : roots) {
	        List<List<BranchNode>> paths = new ArrayList<>();
	        explorePaths(root, new ArrayList<>(), paths);

	        for (List<BranchNode> path : paths) {
	            if (path.size() >= 1) {
	                BranchNode first = path.get(0);
	                BranchNode last = path.get(path.size() - 1);

	                // Avoid duplicate or trivial pairs
	                if (!first.equals(last)) {
	                	writer.write("\n"+pairCount++ + ") " + first.id + " -> " + last.id);
	                }
	            }
	        }
	    }
	    writer.write("\n===============================\n");
		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
	}

	
	
	
	/** Node class representing a control structure in the CDG */
	static class BranchNode {
		String id;
		String type;
		String condition;
		List<BranchNode> dependents = new ArrayList<>();

		BranchNode(String id, String type, String condition) {
			this.id = id;
			this.type = type;
			this.condition = condition;
		}

		void addDependent(BranchNode node) {
			dependents.add(node);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(id).append(" [").append(type).append("] -> ")
			.append(condition);
			if (!dependents.isEmpty()) {
				sb.append(" | dependents: ");
				for (BranchNode dep : dependents) {
					sb.append(dep.id).append(" ");
				}
			}
			return sb.toString();
		}
	}

	/** 
	 * Convenience method to run analysis and output CDG
	 * @param unitComponent 
	 */
	public void generateGraphFile(String unitComponent) throws Exception {
		translationUnit.accept(this);
		saveToFile(unitComponent);
		exportToDotFile(unitComponent);
	}
	/**
	 * Collects all interesting branch pairs (first -> last) to annotate on the DOT graph.
	 */
	private List<String[]> getInterestingBranchPairsForDot() {
	    List<String[]> pairs = new ArrayList<>();

	    Set<BranchNode> allDependents = new HashSet<>();
	    for (BranchNode n : nodes) {
	        allDependents.addAll(n.dependents);
	    }

	    List<BranchNode> roots = new ArrayList<>();
	    for (BranchNode n : nodes) {
	        if (!allDependents.contains(n) && n.id.matches("(Branch\\d+)(-true)?")) {
	            roots.add(n);
	        }
	    }

	    for (BranchNode root : roots) {
	        List<List<BranchNode>> paths = new ArrayList<>();
	        explorePaths(root, new ArrayList<>(), paths);

	        for (List<BranchNode> path : paths) {
	            if (path.size() >= 2) {
	                BranchNode first = path.get(0);
	                BranchNode last = path.get(path.size() - 1);
	                if (!first.equals(last)) {
	                    pairs.add(new String[]{first.id, last.id});
	                }
	            }
	        }
	    }
	    return pairs;
	}

}
