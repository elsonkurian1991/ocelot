package it.unisa.ocelot.suites;

import it.unisa.ocelot.genetic.objectives.GenericObjective;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	// -----------------------------------------------------------------------
	// UCB1 exploration constant.
	// sqrt(2) is the theoretically optimal value for UCB1.
	// Increase to explore more (try less-attempted objectives more often).
	// Decrease to exploit more (focus on objectives already showing progress).
	// --- Change this formula / constant here if needed ---
	// -----------------------------------------------------------------------
	private static final double EXPLORATION_CONSTANT = Math.sqrt(2.0);

	// Weight applied to velocity bonus on top of UCB score.
	// Keeps velocity as a tiebreaker rather than dominating the UCB signal.
	// --- Tune this if velocity should have more/less influence ---
	private static final double VELOCITY_WEIGHT = 0.1;

	// Per-objective statistics tracked across iterations
	// Key = objectiveID
	private final Map<Integer, ObjectiveStats> statsMap;

	// Total number of (objective, iteration) attempts across all objectives
	private int totalAttempts;

	public MABController() {
		this.statsMap = new HashMap<>();
		this.totalAttempts = 0;
	}

	// -----------------------------------------------------------------------
	// Step 1: Select the next subset of objectives for MOSA
	// -----------------------------------------------------------------------

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
		List<ScoredObjective> scored = new ArrayList<>(uncoveredObjectives.size());
		for (GenericObjective obj : uncoveredObjectives) {
			double score = computeUCBScore(obj);
			scored.add(new ScoredObjective(obj, score));
		}

		// Sort descending by UCB score — highest priority first
		scored.sort(Comparator.comparingDouble(ScoredObjective::getScore).reversed());

		// Take the top subsetSize objectives
		int count = Math.min(subsetSize, scored.size());
		List<GenericObjective> subset = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			subset.add(scored.get(i).objective);
		}

		return subset;
	}

	// -----------------------------------------------------------------------
	// Step 2: Compute statistics (velocity, fitness improvement)
	// -----------------------------------------------------------------------

	/**
	 * Computes velocity for each objective in the subset after a MOSA run.
	 *
	 * <p>
	 * Velocity = rate of fitness improvement across the run:
	 * 
	 * <pre>
	 * velocity(i) = (firstSnapshotFitness - lastSnapshotFitness) / evaluationsDelta
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
			List<double[]> snapshots = obj.fitnessSnapshots;

			if (snapshots == null || snapshots.size() < 2) {
				// Not enough data points — velocity stays at previous value
				obj.velocity = 0.0;
				continue;
			}

			double[] first = snapshots.get(0);
			double[] last = snapshots.get(snapshots.size() - 1);

			double fitnessDelta = first[1] - last[1]; // improvement (positive = better)
			double evaluationDelta = last[0] - first[0];

			if (evaluationDelta <= 0) {
				obj.velocity = 0.0;
				continue;
			}

			// Normalise to [0, 1] range — fitness values are already in [0, 1]
			// --- Velocity formula — change here if needed ---
			obj.velocity = fitnessDelta / evaluationDelta;
		}
	}

	// -----------------------------------------------------------------------
	// Step 3: Record results and update MAB statistics after each iteration
	// -----------------------------------------------------------------------

	/**
	 * Records the result of a MOSA iteration for each objective in the subset.
	 * Updates attempt counts and average reward used by UCB1 in the next
	 * iteration's selection.
	 *
	 * <p>
	 * Reward per objective = coverage gain contributed / evaluations spent. An
	 * objective that was covered this iteration gets full reward; one that showed
	 * no progress gets reward 0.
	 *
	 * @param subsetObjectives objectives passed to MOSA this iteration
	 * @param totalEvaluations total evaluations MOSA spent this iteration
	 */
	public void recordResult(List<GenericObjective> subsetObjectives, int totalEvaluations) {

		for (GenericObjective obj : subsetObjectives) {
			int id = obj.getObjectiveID();

			// Initialise stats entry on first encounter
			statsMap.putIfAbsent(id, new ObjectiveStats());
			ObjectiveStats stats = statsMap.get(id);

			// Reward = 1.0 if covered this iteration, else proportional to
			// fitness improvement (velocity signal).
			// --- Reward formula — change here if needed ---
			double reward;
			if (obj.isCovered()) {
				reward = 1.0; // maximum reward for full coverage
			} else if (totalEvaluations > 0) {
				// Partial reward proportional to velocity (how much progress was made)
				reward = Math.max(0.0, obj.velocity);
			} else {
				reward = 0.0;
			}

			stats.addAttempt(reward);
			totalAttempts++;
		}
	}

	// -----------------------------------------------------------------------
	// Internal: UCB1 score computation
	// -----------------------------------------------------------------------

	/**
	 * Computes the UCB1 score for a single objective. Objectives never attempted
	 * before return Double.MAX_VALUE to guarantee they are tried at least once
	 * (exploration priority).
	 */
	private double computeUCBScore(GenericObjective obj) {
		int id = obj.getObjectiveID();
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
		double velocityBonus = VELOCITY_WEIGHT * obj.velocity;

		return exploitation + exploration + velocityBonus;
	}

	// -----------------------------------------------------------------------
	// Internal helper classes
	// -----------------------------------------------------------------------

	/** Tracks attempt count and cumulative reward for one objective. */
	private static class ObjectiveStats {
		int attempts = 0;
		double totalReward = 0.0;

		void addAttempt(double reward) {
			attempts++;
			totalReward += reward;
		}

		double avgReward() {
			return attempts == 0 ? 0.0 : totalReward / attempts;
		}
	}

	/** Pairs an objective with its computed UCB score for sorting. */
	private static class ScoredObjective {
		final GenericObjective objective;
		final double score;

		ScoredObjective(GenericObjective objective, double score) {
			this.objective = objective;
			this.score = score;
		}

		double getScore() {
			return score;
		}
	}
}