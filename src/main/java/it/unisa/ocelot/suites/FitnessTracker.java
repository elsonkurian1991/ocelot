package it.unisa.ocelot.suites;

import it.unisa.ocelot.genetic.objectives.GenericObjective;
import java.util.Locale;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Tracks and persists per-generation fitness snapshots for ALL objectives
 * across all outer iterations to a single CSV file.
 *
 * CSV columns:
 *   outerIteration, generation, evaluations, objectiveID, bestFitness, isCovered, inSubset
 *
 * For objectives IN the subset  : one row per MOSA generation (from fitnessSnapshots).
 * For objectives NOT in subset  : one row per generation using last known bestFitness,
 *                                 so ALL objectives appear in every iteration's data.
 * inSubset=true  → MOSA was actively optimising this objective this iteration.
 * inSubset=false → objective was idle; fitness shown is carry-over from last run.
 */
public class FitnessTracker {

	private static final String CSV_FILENAME = "fitness_progress.csv";
	private static final String CSV_HEADER   =
			"outerIteration,generation,evaluations,objectiveID,bestFitness,isCovered,inSubset";

	// Converts per-iteration local evaluation counts to a continuous global x-axis
	private int cumulativeEvalOffset;

	private final PrintWriter writer;
	// Idle-objective rows are 7000+ per iteration; keep off unless you need them.
	private boolean includeIdleObjectives = false;
	public void setIncludeIdleObjectives(boolean v) { this.includeIdleObjectives = v; }
	private final boolean enabled;

	/** Returns a tracker that does nothing: no file is created, flush/close are no-ops. */
	public static FitnessTracker disabled() {
	    return new FitnessTracker();
	}
	private FitnessTracker() {
	    this.enabled = false;
	    this.writer = null;
	    this.cumulativeEvalOffset = 0;
	}
	/**
	 * Creates the CSV file and writes the header.
	 * Call once before the outer iteration loop.
	 *
	 * @param outputDir directory to write fitness_progress.csv
	 */
	public FitnessTracker(String outputDir) throws IOException {
		this.enabled = true;
		this.cumulativeEvalOffset = 0;

		File dir = new File(outputDir);
		if (!dir.exists()) dir.mkdirs();

		File csvFile = new File(dir, CSV_FILENAME);
		// Overwrite mode — fresh file per tool execution
		this.writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFile, false)));
		writer.println(CSV_HEADER);
		writer.flush();

		System.out.println("[FitnessTracker] Writing to: " + csvFile.getAbsolutePath());
	}

	/**
	 * Flushes per-generation fitness snapshots for ALL objectives to CSV.
	 * Called by GenAndWrite after each outer MOSA iteration.
	 *
	 * @param outerIteration   current outer iteration number (1-based)
	 * @param allObjectives    the FULL objective list (all 200)
	 * @param subsetObjectives objectives that were in the MOSA subset this iteration
	 */
	public void flush(int outerIteration,
			List<GenericObjective> allObjectives,
			List<GenericObjective> subsetObjectives) {
		if (!enabled) return;
		// Build fast lookup of subset IDs
		Set<Integer> subsetIds = new HashSet<>();
		for (GenericObjective obj : subsetObjectives) {
			subsetIds.add(obj.getObjectiveID());
		}

		// Find how many generations MOSA ran and what the max local eval was
		int generationsThisIteration = 0;
		int maxEvalThisIteration     = 0;

		for (GenericObjective obj : subsetObjectives) {
			List<double[]> fitnessSnapshots = obj.getSnapshots();
			if (fitnessSnapshots != null && !fitnessSnapshots.isEmpty()) {
				generationsThisIteration = fitnessSnapshots.size();
				double[] last = fitnessSnapshots.get(generationsThisIteration - 1);
				int localEval = (int) last[1];
				if (localEval > maxEvalThisIteration) {
					maxEvalThisIteration = localEval;
				}
				break; // all subset objectives have the same generation count
			}
		}

		// Write subset objectives — use their per-generation snapshots
		// snapshot format: [generation, localEvaluations, fitness]
		for (GenericObjective obj : subsetObjectives) {
			List<double[]> snapshots = obj.getSnapshots();

			if (snapshots == null || snapshots.isEmpty()) {
				// Fallback: no snapshots recorded (very short budget)
			    writeRow(outerIteration, 0, cumulativeEvalOffset,
			            obj.getObjectiveID(),
			            obj.isCovered() ? 0.0 : 1.0,
			            true);
			    continue;
			}

			for (double[] snapshot : snapshots) {
				int    generation = (int) snapshot[0];
				int    localEval  = (int) snapshot[1];
				double fitness    = snapshot[2];

				writeRow(outerIteration, generation, cumulativeEvalOffset + localEval,
			            obj.getObjectiveID(), fitness, true);
			}
		}

		// Write non-subset objectives — one row per generation using their
		// last known bestFitness so the plot shows all objectives every iter
		/*int genCount = Math.max(1, generationsThisIteration);

		for (GenericObjective obj : allObjectives) {
			if (subsetIds.contains(obj.getObjectiveID())) continue; // already written

			double lastFitness = obj.isCovered() ? 0.0
					: (obj.getBestFitness() == Double.MAX_VALUE ? 1.0 : obj.getBestFitness());

			for (int gen = 1; gen <= genCount; gen++) {
				// Approximate global eval proportionally across generations
				int approxEval = maxEvalThisIteration > 0
						? cumulativeEvalOffset + (gen * maxEvalThisIteration / genCount)
								: cumulativeEvalOffset;

				writeRow(outerIteration, gen, approxEval,
			            obj.getObjectiveID(), lastFitness, false);
			}
		}*/
		// Idle objectives: ONE summary row per iteration (last generation) instead of one
		// row per generation. Per-generation rows for thousands of idle objectives made
		// the CSV grow by hundreds of millions of rows.
		if (includeIdleObjectives) {
		    int lastGen = Math.max(1, generationsThisIteration);
		    for (GenericObjective obj : allObjectives) {
		        if (subsetIds.contains(obj.getObjectiveID())) continue;
		        double lastFitness = obj.isCovered() ? 0.0
		                : (obj.getBestFitness() == Double.MAX_VALUE ? 1.0 : obj.getBestFitness());
		        writeRow(outerIteration, lastGen, cumulativeEvalOffset + maxEvalThisIteration,
		                obj.getObjectiveID(), lastFitness, false);
		    }
		}
		writer.flush();

		// Advance offset so next iteration's evals continue from here
		cumulativeEvalOffset += maxEvalThisIteration;

		System.out.println("[FitnessTracker] Flushed iteration " + outerIteration
				+ " | generations: " + generationsThisIteration
				+ " | cumulative evals: " + cumulativeEvalOffset);
	}
	
	/** Writes one CSV row. isCovered is derived from this row's fitness, not the final state. */
	private void writeRow(int outerIteration, int generation, int evaluations,
	        int objectiveId, double fitness, boolean inSubset) {
	    writer.printf(Locale.ROOT, "%d,%d,%d,%d,%.6f,%s,%s%n",
	            outerIteration, generation, evaluations,
	            objectiveId, fitness, fitness == 0.0, inSubset);
	}
	
	/** Closes the CSV writer. Call once after all iterations complete. */
	public void close() {
		if (writer != null) {
			writer.flush();
			writer.close();
			System.out.println("[FitnessTracker] CSV file closed.");
		}
	}
}