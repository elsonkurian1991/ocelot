package it.unisa.ocelot.c.cdg;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputFilter.Config;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.aspectj.org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;

import io.github.pavelicii.allpairs4j.AllPairs;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairObjective;
public class BranchChainManager {
    private Map<String, List<BranchChain>> allBranchChains;
    private static Map<String, List<BranchChain>> allBranchChainsSaved = new HashMap<String, List<BranchChain>>();
    private ConfigManager config;
    private static List<BranchChainPair> allPairs = new ArrayList<>();
    // Default filename for CDG output. The actual path is resolved lazily using test.basedir from ConfigManager.
    private static final String DEFAULT_CDG_FILENAME = "cdg_output.txt";
    // filename for CFG+CDG human-readable dump (same directory as cdg_output.txt)
    private static final String DEFAULT_CFG_CDG_FILENAME = "cfg_cdg.txt";
    // instance-level output files (created lazily so we can read ConfigManager.test.basedir at runtime)
    private File outputFile = null;
    private File cfgCdgFile = null;
    // One-time-per-JVM guard: delete old CDG output file on first call to getOutputFile()
    private static volatile boolean OUTPUT_FILE_CLEANED = false;
    // One-time-per-JVM guard: truncate old cfg_cdg file on first call
    private static volatile boolean CFG_CDG_CLEANED = false;
    public static HashMap<String, Double> newFitnessHashMap = new HashMap<String, Double>();
    public static List<GenericObjective> generatedBranchChainObjectives;

    public BranchChainManager() {
        // TODO Auto-generated constructor stub
        this.allBranchChains=new HashMap<String, List<BranchChain>>();
    }

    /** Helper: append a single line to the cdg output file. Ensures newline termination and closes the writer.
     * Using a small helper keeps all writes consistent and avoids missing newlines.
     */
    private void appendLine(String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputFile(), true))) {
            if (line == null) line = "";
            writer.write(line);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Error writing CDG file: " + e.getMessage());
        }
    }

    /**
     * Lazily create and return the File used to write CDG output. Prefer the configured test.basedir
     * (ConfigManager.getTestBasedir()). If config is not available, attempt to obtain the ConfigManager
     * instance; otherwise fall back to current working directory (./).
     *
     * Additionally: on the first call per JVM run, delete any existing CDG output file so each run starts
     * with a clean file. Also ensure the parent directory exists.
     */
    private File getOutputFile() {
        if (this.outputFile != null)
            return this.outputFile;

        String baseDir = "./";
        if (this.config != null) {
            baseDir = this.config.getTestBasedir();
        } else {
            try {
                ConfigManager cfg = ConfigManager.getInstance();
                baseDir = cfg.getTestBasedir();
            } catch (IOException e) {
                // ignore and use fallback
            }
        }

        if (baseDir == null || baseDir.isEmpty())
            baseDir = "./";
        if (!baseDir.endsWith("/"))
            baseDir = baseDir + "/";

        // Ensure we delete any old CDG output file only once per JVM run.
        if (!OUTPUT_FILE_CLEANED) {
            File candidate = new File(baseDir + DEFAULT_CDG_FILENAME);
            try {
                // Ensure parent directory exists
                File parent = candidate.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                if (candidate.exists()) {
                    boolean deleted = candidate.delete();
                    if (!deleted) {
                        System.err.println("Warning: unable to delete existing CDG output file: " + candidate.getAbsolutePath());
                    }
                }
            } catch (SecurityException se) {
                System.err.println("Warning deleting CDG output file: " + se.getMessage());
            } finally {
                OUTPUT_FILE_CLEANED = true;
            }
        }

        this.outputFile = new File(baseDir + DEFAULT_CDG_FILENAME);
        return this.outputFile;
    }

    /**
     * Lazily create and return the File used to write the CFG+CDG human-readable dump (cfg_cdg.txt).
     * It uses the same base directory as the CDG output file. On the first call per JVM run it truncates
     * existing file (deletes it) so each execution starts clean; subsequent calls append.
     */
    private File getCfgCdgFile() {
        if (this.cfgCdgFile != null)
            return this.cfgCdgFile;

        // Reuse logic to find base dir (same as getOutputFile)
        String baseDir = "./";
        if (this.config != null) {
            baseDir = this.config.getTestBasedir();
        } else {
            try {
                ConfigManager cfg = ConfigManager.getInstance();
                baseDir = cfg.getTestBasedir();
            } catch (IOException e) {
                // ignore and use fallback
            }
        }

        if (baseDir == null || baseDir.isEmpty())
            baseDir = "./";
        if (!baseDir.endsWith("/"))
            baseDir = baseDir + "/";

        File candidate = new File(baseDir + DEFAULT_CFG_CDG_FILENAME);
        try {
            // Ensure parent directory exists
            File parent = candidate.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // On first call, truncate/delete existing file to start fresh for this JVM run
            if (!CFG_CDG_CLEANED) {
                if (candidate.exists()) {
                    boolean deleted = candidate.delete();
                    if (!deleted) {
                        System.err.println("Warning: unable to delete existing cfg_cdg file: " + candidate.getAbsolutePath());
                    }
                }
                CFG_CDG_CLEANED = true;
            }
        } catch (SecurityException se) {
            System.err.println("Warning handling cfg_cdg file: " + se.getMessage());
        }

        this.cfgCdgFile = candidate;
        return this.cfgCdgFile;
    }

    /**
     * Write a human-readable dump of the provided CFG and CDG to cfg_cdg.txt in the same directory as cdg_output.txt.
     * The file is truncated once per JVM run (first call) and subsequent calls append. The output contains clear
     * headings and labelled node/edge entries for easy human inspection.
     *
     * @param cfg The control-flow graph to dump
     * @param cdg The control-dependence graph to dump
     * @param componentName Logical name of the component/function being dumped (included in headings)
     */
    public void writeCfgAndCdg(CFG cfg, CDG cdg, String componentName) {
        File out = getCfgCdgFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out, true))) {
            writer.write("");
            writer.newLine();
           writer.write("========================= CFG: " + componentName + " =====================");
            writer.newLine();

            // Print CFG nodes
            writer.write("-- NODES (id: textual content) --");
            writer.newLine();
            List<CFGNode> nodeList = new ArrayList<>(cfg.vertexSet());
            // sort by id for stable output
            Collections.sort(nodeList);
            for (CFGNode node : nodeList) {
                writer.write("Node " + node.getId() + ":");
                writer.newLine();
                String[] lines = node.toString().split("\r?\n");
                for (String ln : lines) {
                    writer.write("    " + ln);
                    writer.newLine();
                }
            }

            // Print CFG edges
            writer.write("-- EDGES (sourceId -> targetId : label [objectiveID]) --");
            writer.newLine();
            // iterate edges in stable order by sourceId then targetId
            List<Object> edges = new ArrayList<>(cfg.edgeSet());
            edges.sort((o1, o2) -> {
                it.unisa.ocelot.c.cfg.edges.LabeledEdge e1 = (it.unisa.ocelot.c.cfg.edges.LabeledEdge)o1;
                it.unisa.ocelot.c.cfg.edges.LabeledEdge e2 = (it.unisa.ocelot.c.cfg.edges.LabeledEdge)o2;
                CFGNode s1 = cfg.getEdgeSource(e1);
                CFGNode s2 = cfg.getEdgeSource(e2);
                int cmp = Integer.compare(s1.getId(), s2.getId());
                if (cmp != 0) return cmp;
                CFGNode t1 = cfg.getEdgeTarget(e1);
                CFGNode t2 = cfg.getEdgeTarget(e2);
                return Integer.compare(t1.getId(), t2.getId());
            });
            for (Object eo : edges) {
                it.unisa.ocelot.c.cfg.edges.LabeledEdge e = (it.unisa.ocelot.c.cfg.edges.LabeledEdge)eo;
                CFGNode s = cfg.getEdgeSource(e);
                CFGNode t = cfg.getEdgeTarget(e);
                String lbl = e.getLabel() == null ? "" : e.getLabel().toString();
                writer.write("Edge " + s.getId() + " -> " + t.getId() + " : '" + lbl + "' [objID=" + e.getObjectiveID() + "]");
                writer.newLine();
            }

            // Print CDG
            writer.write("");
            writer.newLine();
            writer.write("========================= CDG: " + componentName + " =====================");
            writer.newLine();

            // CDG nodes
            writer.write("-- CDG NODES (cdgId -> original CFG id : label preview) --");
            writer.newLine();
            List<CDGNode> cdgNodes = new ArrayList<>(cdg.vertexSet());
            cdgNodes.sort(Comparator.comparingInt(CDGNode::getId));
            for (CDGNode cn : cdgNodes) {
                String preview = cn.getLabel() == null ? "" : cn.getLabel().replaceAll("\r?\n", " ");
                int originalId = -1;
                if (cn.getOriginalCFGNode() != null) originalId = cn.getOriginalCFGNode().getId();
                writer.write("CDGNode " + cn.getId() + " -> CFGNode " + originalId + " : " + preview);
                writer.newLine();
            }

            // CDG edges
            writer.write("-- CDG EDGES (sourceCdgId -> targetCdgId : label) --");
            writer.newLine();
            List<ControlDependenceEdge> cdgEdges = new ArrayList<>(cdg.edgeSet());
            for (ControlDependenceEdge ce : cdgEdges) {
                CDGNode s = cdg.getEdgeSource(ce);
                CDGNode t = cdg.getEdgeTarget(ce);
                String label = (ce == null) ? "FLOW" : ce.toString();
                writer.write("CDGEdge " + (s == null ? "?" : s.getId()) + " -> " + (t == null ? "?" : t.getId()) + " : '" + label + "'");
                writer.newLine();
            }

            writer.write("-- END DUMP for: " + componentName + " --");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing cfg_cdg file: " + e.getMessage());
        }
    }

    public void Process(CFG cfg, String tempUnitComponent,Map<IASTExpression, Integer> branchChainsMap) throws Exception {
        this.config = ConfigManager.getInstance();
        this.allBranchChains=new HashMap<String, List<BranchChain>>();
    //build the CDG graph from CFG
        // CRITICAL: Reset CFGNode and CDGNode IDs for each unit component
        CFGNode.reset();      // Your existing CFGNode reset method
        CDG.resetNodeIds();   // New CDGNode reset method
        CDG cdg = new CDG(cfg);
        BranchChainExtractor extractor = new BranchChainExtractor(cdg,tempUnitComponent,branchChainsMap);
        List<BranchChain> chains = extractor.extractBranchChains();//// Get AST-based representation (for fitness calculation)
        // Get text representation (for debugging)
        String textOutput = extractor.extractBranchChainsText();
        allBranchChains.put(tempUnitComponent, chains);
        System.out.println("Processed " + tempUnitComponent + ": " + chains.size() + " branch-chains");

        // Write a consistent header and the textual representation, ensuring newlines are present
        appendLine("");
        appendLine("=========================Control Dependence Graph: " + tempUnitComponent + "=====================" );
        System.out.println(textOutput);
        // write textOutput; ensure terminated by newline(s)
        if (textOutput != null && !textOutput.isEmpty()) {
            for (String line : textOutput.split("\r?\n")) {
                appendLine(line);
            }
        }

        appendLine("Extracted " + chains.size() + " branch-chains:");
        for (BranchChain chain : chains) {
            String line = "  - " + chain.getLabel() + " (to leaf node " + chain.getLeafNode().getId() + ")";
            System.out.println(line);
            appendLine(line);
        }

        // Also write a human-readable CFG+CDG dump to cfg_cdg.txt (same directory as cdg_output.txt)
        try {
            writeCfgAndCdg(cfg, cdg, tempUnitComponent);
        } catch (Exception e) {
            System.err.println("Warning: failed to write cfg_cdg dump: " + e.getMessage());
        }

        // store chains for later pairing
        allBranchChainsSaved.put(tempUnitComponent, chains);
        System.err.println(allBranchChains);
        System.err.println(allBranchChainsSaved); //map where all chains are saved. 
    }

    public void generatePairsForBranchChains() {

        // ensure config available
        if (this.config == null) {
            try {
                this.config = ConfigManager.getInstance();
            } catch (IOException e) {
                System.err.println("Unable to load configuration for pair generation: " + e.getMessage());
                appendLine("ERROR: Unable to load configuration: " + e.getMessage());
                return;
            }
        }

        // Get component pairs from configuration (preserves duplicates)
        List<ComponentPair> componentPairs = this.config.getPairComponents();
        appendLine("");
        appendLine("=== GENERATING BRANCH-CHAIN PAIRS ===");
        appendLine("Component pairs from configuration: " + componentPairs.size());
        for (ComponentPair componentPair : componentPairs) {
            String component1 = componentPair.getComponent1();
            String component2 = componentPair.getComponent2();

            System.out.println("\nProcessing pair: " + component1 + " <-> " + component2);
            appendLine("Processing pair: " + component1 + " <-> " + component2);
            // Get branch-chains for both components
            List<BranchChain> chains1 = allBranchChainsSaved.get(component1);
            List<BranchChain> chains2 = allBranchChainsSaved.get(component2);

            // Validate both components exist
            if (chains1 == null) {
                System.err.println("WARNING: Component '" + component1 + "' not found in processed chains");
                appendLine("WARNING: Component '" + component1 + "' not found in processed chains");
                continue;
            }
            if (chains2 == null) {
                System.err.println("WARNING: Component '" + component2 + "' not found in processed chains");
                appendLine("WARNING: Component '" + component2 + "' not found in processed chains");
                continue;
            }

            // Generate Cartesian product
            List<BranchChainPair> pairsForThisComponent = generateCartesianProduct(
                    component1, chains1, 
                    component2, chains2
                    );

            allPairs.addAll(pairsForThisComponent);
            // write the generated pairs in a readable form (one per line) instead of relying on List.toString()
            appendLine("Generated pairs for: " + component1 + " <-> " + component2 + " (count=" + pairsForThisComponent.size() + ")");
            for (BranchChainPair p : pairsForThisComponent) {
                appendLine("  - " + p.toString());
            }
            System.out.println("  Generated " + pairsForThisComponent.size() + " branch-chain pairs");
            appendLine("  Generated " + pairsForThisComponent.size() + " branch-chain pairs");
        }

        System.out.println("\nTotal branch-chain pairs generated: " + allPairs.size());
        appendLine("Total branch-chain pairs generated: " + allPairs.size());
    }

    /**
     * Generates Cartesian product of branch-chains between two components.
     *
     * @param component1 First component name
     * @param chains1 Branch-chains of first component
     * @param component2 Second component name
     * @param chains2 Branch-chains of second component
     * @return List of all possible pairs
     */
    private List<BranchChainPair> generateCartesianProduct(
            String component1, List<BranchChain> chains1,
            String component2, List<BranchChain> chains2) {

        List<BranchChainPair> pairs = new ArrayList<>();

        // Cartesian product: for each chain in component1, pair with each chain in component2
        for (BranchChain chain1 : chains1) {
            for (BranchChain chain2 : chains2) {
                BranchChainPair pair = new BranchChainPair(
                        component1, chain1,
                        component2, chain2
                        );
                pairs.add(pair);
            }
            // Log the set of chains for debugging; use appendLine for consistent formatting
            appendLine("");
            appendLine(" Cartesian product: for each chain in component1, pair with each chain in component2 ");
            appendLine("Chains1 (" + component1 + "): " + chains1.toString());
            appendLine("Chains2 (" + component2 + "): " + chains2.toString());
        }

        return pairs;
    }
    public static  void cacheFitnessValues() {
        newFitnessHashMap.clear();
        
        try (BufferedReader f_Val_File = new BufferedReader(new FileReader("./fitnessValues.txt"))) {
            String lineBr = f_Val_File.readLine();
            while (lineBr != null) {
                //System.err.println(lineBr);
                FunBranchNameAndFitness infoFromLinebr = readInfoFromLine(lineBr);
                if(newFitnessHashMap.containsKey(infoFromLinebr.getFunBranchName()) && newFitnessHashMap.get(infoFromLinebr.getFunBranchName()) < infoFromLinebr.getCurrFitnessVal()) {
                    // Do nothing
                }
                else {
                    newFitnessHashMap.put(infoFromLinebr.getFunBranchName(), infoFromLinebr.getCurrFitnessVal());
                }
                //allBranchChainsSaved.get(infoFromLinebr.getFunBranchName());
                lineBr = f_Val_File.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error reading fitnessValues.txt file: " + e.getMessage());
        }
    }
            private static FunBranchNameAndFitness readInfoFromLine(String lineBr) {
                FunBranchNameAndFitness infoFromLinebr = new FunBranchNameAndFitness();
                String listOfItems[] = lineBr.split(";");
                String fName = listOfItems[0];
                String branchName = listOfItems[1];
                String fitnessVal = listOfItems[2];
                String fun_BranchName = fName + ":" + branchName;
                fitnessVal = fitnessVal.replace(",", ".");
                double currFitness = Double.parseDouble(fitnessVal);
                if (currFitness > 1)
                    System.err.println("Wrong fitness value! Branch:" + fun_BranchName + " Fitness:" + currFitness);
                infoFromLinebr.setFunBranchName(fun_BranchName);
                infoFromLinebr.setCurrFitnessVal(currFitness);
                return infoFromLinebr;
            }

            public static List<GenericObjective> loadObjectives() {
                // TODO Auto-generated method stub
                if (generatedBranchChainObjectives == null) {
                    //public static List<BranchChainPairStateMachine> ListOfSMs;
                    List<GenericObjective> objectives = new ArrayList<GenericObjective>();
                    int j = 0;
                    int objectiveID = 0;
                    for (BranchChainPair ListOfBranchChains : allPairs) {
                        BranchChainPairStateMachine BC_Pair = new BranchChainPairStateMachine(objectiveID,ListOfBranchChains.getChain1(), ListOfBranchChains.getChain2());
                        //PC_PairObjective PC_Pair = new PC_PairObjective(false, objectiveID, sm, "Forward", indirectionLevel);
                        objectives.add(BC_Pair);
                        objectiveID++;
                        //objectives.add(newObj);
                    }

                    generatedBranchChainObjectives=(List<GenericObjective>)objectives;
                    System.out.println(generatedBranchChainObjectives);
                    return generatedBranchChainObjectives ;
                }
                else {
                    return generatedBranchChainObjectives ;
                }
            }



}