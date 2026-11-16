package jmetal.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;

/**
 * This class implements some facilities for ranking solutions. Given a
 * {@link SolutionSet}, the {@link Solution}s in it are ranked according to
 * the scheme proposed by Panichella et al in "Reformulating Branch Coverage 
 * as a Many-Objective Optimization Problem". The
 * first non dominated front, having rank 0, is obtained with their proposed
 * preference criterion. Remaining solution are ranked similarly as proposed in
 * NGSA-II, with the difference that the we consider non-dominance relation only
 * for uncovered branches, using {@link MOSADominanceComparator}.
 * 
 * @author giograno
 */
public class MOSARanking {
	/**
	 * The {@link SolutionSet} to rank.
	 */
	private final SolutionSet solutionSet_;

	/**
	 * Stores the branches that are yet to cover.
	 */
	private final Set<LabeledEdge> uncoveredBranches_;

	/**
	 * Stores a {@link MOSADominanceComparator} for dominance checking.
	 */
	private final MOSADominanceComparator dominanceComparator_;

	/**
	 * An array mapping a rank to a {@link SolutionSet}
	 * containing all the {@link Solution}s in {@link #solutionSet_}
	 * that have that rank (in other words, maps a
	 * rank to the content of the corresponding subfront).
	 */
	private final SolutionSet[] ranking_;
	
	/**
	 * Constructor.
	 * 
	 * @param pSolutionSet the {@link SolutionSet} to rank.
	 * @param pTargets a {@link Set}{@code <}{@link LabeledEdge}{@code >} 
	 *        containing all the targets. It must be a subset of the 
	 *        branches in the CFG. Covered targets must be marked.
	 */
	@SuppressWarnings("unchecked")
	public MOSARanking(SolutionSet pSolutionSet, Set<LabeledEdge> pTargets) {
		this.solutionSet_ = pSolutionSet;
		this.uncoveredBranches_ = new HashSet<>(pTargets);
		this.dominanceComparator_ = new MOSADominanceComparator(this.uncoveredBranches_);

		// iDominate[i] contains the list of solutions dominated by i
		List<Integer>[] iDominate = new List[this.solutionSet_.size()];

		// dominateMe[i] contains the number of solutions dominating i
		int[] dominateMe = new int[this.solutionSet_.size()];

		// Initialize iDominate and dominateMe
		for (int p = 0; p < this.solutionSet_.size(); p++) {
			iDominate[p] = new LinkedList<>();
			dominateMe[p] = 0;
		}
		
		// frontIndividuals[i] contains the list of individuals belonging to the front i
		List<Integer>[] frontIndividuals = new List[this.solutionSet_.size() + 1];

		// Initialize frontIndividuals
		for (int i = 0; i < frontIndividuals.length; i++) {
			frontIndividuals[i] = new LinkedList<>();
		}

		// flagDominate is an auxiliary encodings.variable
		int flagDominate;

		/*** Fast non dominated sorting algorithm ***/

		// Scan all the p individuals
		for (int p = 0; p < this.solutionSet_.size() - 1; p++) {
			// For all q individuals after p, calculate if p dominates q or vice versa
			for (int q = p + 1; q < this.solutionSet_.size(); q++) {
				// check dominance between solutions p and q
				flagDominate = this.dominanceComparator_.compare(pSolutionSet.get(p), pSolutionSet.get(q));

				// update iDominate and dominateMe
				if (flagDominate == -1) {
					iDominate[p].add(q);
					dominateMe[q]++;
				} else if (flagDominate == 1) {
					iDominate[q].add(p);
					dominateMe[p]++;
				}
			}
		}

		// n.b: first front normally is on rank 0, so in MOSA 
		// rank 0 is reserved for MOSA preference criterion

		// if nobody dominates p, p belongs to the first front
		for (int p = 0; p < this.solutionSet_.size(); p++) {
			if (dominateMe[p] == 0) {
				frontIndividuals[0].add(p);
				pSolutionSet.get(p).setRank(1); //rank 0 for best case
			}
		}

		// Obtain the rest of fronts
		int i = 0; //counts the fronts
		while (frontIndividuals[i].size() != 0) {
			for (int individual : frontIndividuals[i]) {
				for (int index : iDominate[individual]) {
					dominateMe[index]--;
					if (dominateMe[index] == 0) {
						frontIndividuals[i + 1].add(index);
						this.solutionSet_.get(index).setRank(i + 2);
					}
				}
			}
			i++;
		}

		// Build the fronts
		this.ranking_ = new SolutionSet[i];
		//0, 1, 2, .... , i - 1 are the fronts (so there are i fronts in total)
		for (int k = 0; k < i; k++) {
			this.ranking_[k] = new SolutionSet(frontIndividuals[k].size());
			for (int individual : frontIndividuals[k]) {
				this.ranking_[k].add(pSolutionSet.get(individual));
			}
		}
	}

	/**
	 * Returns a {@link SolutionSet} containing the 
	 * solutions of a given rank.
	 * 
	 * @param rank an {@code int}. It must range
	 *        between 0 and {@link #getNumberOfSubfronts()}{@code - 1}.
	 * @return the {@link SolutionSet} containing all
	 *         the {@link Solution}s in the subfront 
	 *         with rank {@code rank}.
	 */
	public SolutionSet getSubfront(int rank) {
		return this.ranking_[rank];
	}

	/**
	 * Returns the total number of subfronts found.
	 * 
	 * @return an {@code int}.
	 */
	public int getNumberOfSubfronts() {
		return this.ranking_.length;
	}
}
