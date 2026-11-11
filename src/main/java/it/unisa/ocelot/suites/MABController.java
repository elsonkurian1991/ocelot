package it.unisa.ocelot.suites;

import it.unisa.ocelot.genetic.objectives.GenericObjective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Multi-Armed Bandit (MAB) controller for selecting which subset of uncovered
 * objectives to pass to MOSA in each iteration.
 *
 * <p>
 * <b>Algorithm:</b> UCB1 (Upper Confidence Bound), which balances:
 * <ul>
 * <li><b>Exploitation</b> — prioritise objectives we are making fast progress
 * on</li>
 * <li><b>Exploration</b> — try objectives we haven't attempted much yet</li>
 * </ul>
 *
 * <p>
 * <b>UCB1 score per objective:</b>
 * 
 * <pre>
 * score(i) = avgReward(i) + C * sqrt(ln(totalAttempts) / attempts(i))
 * </pre>
 * 
 * Where:
 * <ul>
 * <li>{@code avgReward(i)} = average coverage gain per evaluation for objective
 * i</li>
 * <li>{@code C} = exploration constant (default sqrt(2) per UCB1 theory)</li>
 * <li>{@code totalAttempts} = total number of times ANY objective was
 * attempted</li>
 * <li>{@code attempts(i)} = number of iterations objective i was included
 * in</li>
 * </ul>
 *
 * <p>
 * The velocity of each objective (how fast its fitness improves) is used as a
 * secondary bonus on top of the UCB score, so objectives that are actively
 * improving get a slight preference over stagnant ones with the same UCB score.
 */
public class MABController {

	// UCB1 exploration constant.
	// sqrt(2) is the theoretically optimal value for UCB1.
	// Increase to explore more (try less-attempted objectives more often).
	// Decrease to exploit more (focus on objectives already showing progress).
	//TODO --- Change this formula / constant here if needed ---
	private static final double EXPLORATION_CONSTANT = Math.sqrt(2.0);

	// Weight applied to velocity bonus on top of UCB score.
	// Keeps velocity as a tiebreaker rather than dominating the UCB signal.
	// --- Tune this if velocity should have more/less influence ---
	private static final double VELOCITY_WEIGHT = 0.1;

	// Per-objective statistics tracked across iterations
	// Key = objective, value = stats (attempts, total reward, etc.)
	private final Map<GenericObjective, ObjectiveStats> statsMap;

	// Total number of (objective, iteration) attempts across all objectives
	private int totalAttempts;
	
	private static final double NEVER_TRIED_BASE = 1e9;   // keeps "untried first" priority
	private static final double PROXIMITY_WEIGHT = 0.5;
	
	public MABController() {
		this.statsMap = new HashMap<>();
		this.totalAttempts = 0;
	}


	/**
	 * Selects a subset of objectives for the next MOSA iteration using UCB1.
	 *
	 * <p>
	 * Objectives never attempted before get infinite UCB score (exploration
	 * priority) so they are always tried at least once before exploitation begins.
	 *
	 * @param uncoveredObjectives all objectives not yet covered
	 * @param subsetSize          maximum number of objectives to return (typically
	 *                            populationSize / 2)
	 * @return ordered list of selected objectives, highest UCB score first
	 */
	public List<GenericObjective> selectSubset(List<GenericObjective> uncoveredObjectives, int subsetSize) {

		// Score every uncovered objective
		for (GenericObjective obj : uncoveredObjectives) {
			double score = computeUCBScore(obj);
			this.statsMap.putIfAbsent(obj, new ObjectiveStats(obj));
			ObjectiveStats objStats = this.statsMap.get(obj);
			objStats.score = score;
		}

		// Sort descending by UCB score — highest priority first
		List<ObjectiveStats> scored = new ArrayList<>(this.statsMap.values());
		Collections.sort(scored, Comparator.comparingDouble(ObjectiveStats::getScore).reversed());

		// Take the top subsetSize objectives
		int count = Math.min(subsetSize, scored.size());
		List<GenericObjective> subset = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			subset.add(scored.get(i).objective);
		}

		return subset;
	}

	/**
	 * Computes velocity for each objective in the subset after a MOSA run.
	 *
	 * <p>
	 * Velocity = rate of fitness improvement across the run:
	 * 
	 * <pre>
	 * velocity(i) = velocity(i) = (firstFitness - lastFitness) / firstFitness
	 * </pre>
	 * 
	 * Higher velocity means faster improvement = objective is easier to cover. A
	 * velocity of 0.0 means no improvement was made during this run.
	 *
	 * <p>
	 * This method reads the {@code fitnessSnapshots} list that MOSA_Generic
	 * populates during its run via {@code GenericObjective.recordSnapshot()}.
	 *
	 * @param subsetObjectives the objectives that were passed to MOSA this
	 *                         iteration
	 */
	public void computeVelocities(List<GenericObjective> subsetObjectives) {
		for (GenericObjective obj : subsetObjectives) {
			ObjectiveStats objStats = this.statsMap.get(obj);
			
			List<double[]> snapshots = obj.getSnapshots();

			if (snapshots == null || snapshots.size() < 2) {
				// no data: velocity = 0.0 (no improvement possible)
				objStats.velocity = 0.0;
				continue;
			}

			/*double[] first = snapshots.get(0);
			double[] last = snapshots.get(snapshots.size() - 1);

			double fitnessDelta = first[1] - last[1]; // improvement (positive = better)
			double evaluationDelta = last[0] - first[0];

			if (evaluationDelta <= 0) {
				obj.setVelocity(0.0);
				continue;
			}

			// Normalise to [0, 1] range — fitness values are already in [0, 1]
			// --- Velocity formula — change here if needed ---
			//obj.setVelocity(fitnessDelta / evaluationDelta);
			*/
			double first0 = snapshots.get(0)[2];
			double last0  = snapshots.get(snapshots.size() - 1)[2];

			// Relative improvement over this run, in [0, 1]. All subset objectives share
			// the same evaluation budget, so dividing by evaluations would not change the ranking.
			double improvement = (first0 - last0) / Math.max(first0, 1e-9);
			objStats.velocity = Math.max(0.0, Math.min(1.0, improvement));
			   //System.out.println("[MAB] obj " + obj.getObjectiveID() + " velocity=" + obj.getVelocity());
		}
	}

	/**
	 * Records the result of a MOSA iteration for each objective in the subset.
	 * Updates attempt counts and average reward used by UCB1 in the next
	 * iteration's selection.
	 *
	 * <p>
	 * Reward per objective = 1.0 if covered, otherwise the relative fitness improvement.
	 * @param subsetObjectives objectives passed to MOSA this iteration
	 * @param totalEvaluations total evaluations MOSA spent this iteration
	 */
	public void recordResult(List<GenericObjective> subsetObjectives, int totalEvaluations) {

		for (GenericObjective obj : subsetObjectives) {
				// Initialise stats entry on first encounter
			this.statsMap.putIfAbsent(obj, new ObjectiveStats(obj));
			ObjectiveStats stats = this.statsMap.get(obj);

			// Reward = 1.0 if covered this iteration, else proportional to
			// fitness improvement (velocity signal).
			// --- Reward formula — change here if needed ---
			double reward;
			if (obj.isCovered()) {
				reward = 1.0; // maximum reward for full coverage
			} else if (totalEvaluations > 0) {
				// Partial reward proportional to velocity (how much progress was made)
				reward = 0.5 * Math.max(0.0, stats.getVelocity());
			} else {
				reward = 0.0;
			}

			stats.addAttempt(reward);
			this.totalAttempts++;
		}
	}

	/**
	 * Computes the UCB1 score for a single objective. Objectives never attempted
	 * before return Double.MAX_VALUE to guarantee they are tried at least once
	 * (exploration priority).
	 */
	private double computeUCBScore(GenericObjective obj) {
		ObjectiveStats stats = statsMap.get(obj);
	    double proximityBonus = PROXIMITY_WEIGHT * obj.getProximity();

	    if (stats == null || stats.attempts == 0) {
	        return NEVER_TRIED_BASE + proximityBonus;     // untried: closest first
	    }
	    double exploitation = stats.avgReward();
	    double exploration = EXPLORATION_CONSTANT * Math.sqrt(Math.log(totalAttempts) / stats.attempts);
	    double velocityBonus = VELOCITY_WEIGHT * stats.getVelocity();
	    return exploitation + exploration + velocityBonus + proximityBonus;
		/*int id = obj.getObjectiveID();
		ObjectiveStats stats = statsMap.get(id);

		// Never attempted — give maximum priority so it gets tried at least once
		if (stats == null || stats.attempts == 0) {
			return Double.MAX_VALUE;
		}

		double exploitation = stats.avgReward();

		// UCB1 exploration bonus — decreases as attempts increase
		// --- UCB1 formula — change here if needed ---
		double exploration = EXPLORATION_CONSTANT * Math.sqrt(Math.log(totalAttempts) / stats.attempts);

		// Velocity bonus — slight preference for objectives actively improving
		// --- Velocity bonus formula — change here if needed ---
		double velocityBonus = VELOCITY_WEIGHT * obj.getVelocity();

		return exploitation + exploration + velocityBonus;*/
	}
	
	/** Tracks attempt count and cumulative reward for one objective. */
	private static class ObjectiveStats {
		final GenericObjective objective;
		double score;
		int attempts;
		double totalReward;
		/**
		 * Velocity of fitness improvement for this objective, computed after each
		 * MOSA run by MABController.
		 * velocity = (prevBestFitness - currentBestFitness) / evaluationsDelta
		 * Higher value means the objective is improving faster (easier to cover).
		 * Initialised to 0.0 — unknown velocity before first run.
		 */
		double velocity;
		
		ObjectiveStats(GenericObjective objective) {
			this.objective = objective;
			this.score = 0.0;
			this.attempts = 0;
			this.totalReward = 0.0;
			this.velocity = 0.0;
		}

		void addAttempt(double reward) {
			this.attempts++;
			this.totalReward += reward;
		}

		double avgReward() {
			return this.attempts == 0 ? 0.0 : this.totalReward / this.attempts;
		}

		double getScore() {
			return this.score;
		}
		
		/** Returns the velocity of fitness improvement computed by MABController. */
		public double getVelocity() {
			return this.velocity;
		}

	}
}