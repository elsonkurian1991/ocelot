package it.unisa.ocelot.c.cdg;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.aspectj.org.eclipse.jdt.core.dom.ThisExpression;

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
	// Prefix for all generated output filenames (loaded from config or set to default)
	private static final String CONFIG_FILENAME = ""; // If desired, set a directory or prefix here, e.g. "output/"
	static String filename = CONFIG_FILENAME + "cdg_output.txt";
	private static final File outputFile = new File(filename);
	public static HashMap<String, Double> newFitnessHashMap = new HashMap<String, Double>();
	public static List<GenericObjective> generatedBranchChainObjectives;
	
	public BranchChainManager() {
		// TODO Auto-generated constructor stub
		this.allBranchChains=new HashMap<String, List<BranchChain>>();
	}

	public void Process(CFG cfg, String tempUnitComponent) throws IOException {
		this.config = ConfigManager.getInstance();
		this.allBranchChains=new HashMap<String, List<BranchChain>>();
	//build the CDG graph from CFG
		// CRITICAL: Reset CFGNode and CDGNode IDs for each unit component
		CFGNode.reset();      // Your existing CFGNode reset method
		CDG.resetNodeIds();   // New CDGNode reset method
		CDG cdg = new CDG(cfg);
		BranchChainExtractor extractor = new BranchChainExtractor(cdg,tempUnitComponent);
		List<BranchChain> chains = extractor.extractBranchChains();//// Get AST-based representation (for fitness calculation)
		// Get text representation (for debugging)
		String textOutput = extractor.extractBranchChainsText();
		allBranchChains.put(tempUnitComponent, chains);
		System.out.println("Processed " + tempUnitComponent + ": " + chains.size() + " branch-chains");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			writer.write("\n\n=========================Control Dependence Graph: "+tempUnitComponent+"=========================\n");
			System.out.println(textOutput);
			writer.write(textOutput);
			System.out.println("Extracted " + chains.size() + " branch-chains:");
			writer.write("Extracted " + chains.size() + " branch-chains:");
			for (BranchChain chain : chains) {
				System.out.println("  - " + chain.getLabel() + " (to leaf node " + chain.getLeafNode().getId() + ")");
				writer.write("  - " + chain.getLabel() + " (to leaf node " + chain.getLeafNode().getId() + ")");
			}
		}catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
		/*CDGBuilder cdgBuilder = new CDGBuilder(cfg, tempUnitComponent);//use CFG graph to create CDG graph
		CDG_old cdg = cdgBuilder.buildCDG();
		cdgBuilder.printCDG();
		 */
		//Map<String, String> pairComponents = config.getPairComponents();
		//till here
		allBranchChainsSaved.put(tempUnitComponent, chains);
		System.err.println(allBranchChains);
		System.err.println(allBranchChainsSaved); //map where all chains are saved. 
	}
	public void generatePairsForBranchChains() {

		// Get component pairs from configuration (preserves duplicates)
		List<ComponentPair> componentPairs = this.config.getPairComponents();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			System.out.println("\n=== GENERATING BRANCH-CHAIN PAIRS ===");
			System.out.println("Component pairs from configuration: " + componentPairs.size());
			writer.write("\n=== GENERATING BRANCH-CHAIN PAIRS ===");
			writer.write("\nComponent pairs from configuration: " + componentPairs.size());
			for (ComponentPair componentPair : componentPairs) {
				String component1 = componentPair.getComponent1();
				String component2 = componentPair.getComponent2();

				System.out.println("\nProcessing pair: " + component1 + " <-> " + component2);
				writer.write("\nProcessing pair: " + component1 + " <-> " + component2);
				// Get branch-chains for both components
				List<BranchChain> chains1 = allBranchChainsSaved.get(component1);
				List<BranchChain> chains2 = allBranchChainsSaved.get(component2);

				// Validate both components exist
				if (chains1 == null) {
					System.err.println("WARNING: Component '" + component1 + "' not found in processed chains");
					continue;
				}
				if (chains2 == null) {
					System.err.println("WARNING: Component '" + component2 + "' not found in processed chains");
					continue;
				}

				// Generate Cartesian product
				List<BranchChainPair> pairsForThisComponent = generateCartesianProduct(
						component1, chains1, 
						component2, chains2
						);

				allPairs.addAll(pairsForThisComponent);
				writer.write(pairsForThisComponent.toString());
				System.out.println("  Generated " + pairsForThisComponent.size() + " branch-chain pairs");
				writer.write("\n  Generated " + pairsForThisComponent.size() + " branch-chain pairs");
			}

			System.out.println("\nTotal branch-chain pairs generated: " + allPairs.size());
			writer.write("\nTotal branch-chain pairs generated: " + allPairs.size());
		}catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
		/*try { 
			FileOutputStream fos = new FileOutputStream("allPairsData"); 
			ObjectOutputStream oos = new ObjectOutputStream(fos); 
			oos.writeObject(allPairs); 
			oos.close(); 
			fos.close(); 
		} 
		catch (IOException ioe) { 
			ioe.printStackTrace(); 
		} */
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
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
				writer.write("\n\n Cartesian product: for each chain in component1, pair with each chain in component2 \n");
				writer.write(chains1.toString()+"\n");
				writer.write(chains2.toString()+"\n");
			}catch (IOException e) {
				System.err.println("Error writing CDG file: " + e.getMessage());
			}
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