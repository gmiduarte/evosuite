package org.evosuite.ga.metaheuristics;

import java.util.ArrayList;
import java.util.List;

import org.evosuite.Properties;
import org.evosuite.TimeController;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.archive.ArchiveTestChromosomeFactory;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ElephantHerdingOptimization implementation
 *
 * @author
 */
public class ElephantHerdingOptimization<T extends Chromosome<T>> extends GeneticAlgorithm<T> {

	private static final long serialVersionUID = -8342568601726499012L;
	private final Logger logger = LoggerFactory.getLogger(ElephantHerdingOptimization.class);
	private List<List<T>> clans = new ArrayList<>();

	/**
	 * Constructor
	 *
	 * @param factory a {@link org.evosuite.ga.ChromosomeFactory} object.
	 */
	public ElephantHerdingOptimization(ChromosomeFactory<T> factory) {
		super(factory);

		if (Properties.POPULATION < Properties.NUMBER_OF_ELEPHANT_CLANS) {
			LoggingUtils.getEvoLogger().warn(
					"Number of elephant clans cannot be bigger than population. Value adjusted to be equal to population");
			Properties.NUMBER_OF_ELEPHANT_CLANS = Properties.POPULATION;
		}
	}

	private void sortClans(List<List<T>> list) {
		for (List<T> c : list) {
			sortPopulation(c);
		}
	}

	private void calculateFitnessAndSortClans(List<List<T>> list) {
		for (List<T> c : list) {
			calculateFitnessAndSortPopulation(c);
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected void evolve() {
		List<T> newGeneration = new ArrayList<>();
		List<List<T>> newGenerationClans = new ArrayList<>();
		/*
		 * population -> group of clans -> group of elephants population = nº clans * nº
		 * elephant per clan male leave clan (worst solutions) matriarch (best of each
		 * clan)
		 */

		// update clan phase
		for (int i = 0; i < Properties.NUMBER_OF_ELEPHANT_CLANS; i++) {
			// eq for matriarch of each clan
			List<T> clan = new ArrayList<>();
			T elephant_matriarch = clans.get(i).get(0);
			assert elephant_matriarch != null;

			for (int j = 1; j < clans.get(i).size(); j++) {
				// eq for generic elephant
				T elephant = clans.get(i).get(j).clone();
				try {
					crossoverFunction.crossOver(elephant, elephant_matriarch.clone());
					if (elephant.isChanged()) {
						elephant.updateAge(currentIteration);
					}
					clan.add(elephant);
				} catch (ConstructionFailedException e) {
					logger.info("Crossover failed.");
					clan.add(clans.get(i).get(j));
				}
			}
			notifyMutation(elephant_matriarch);
			elephant_matriarch.mutate();
			if (elephant_matriarch.isChanged()) {
				elephant_matriarch.updateAge(currentIteration);
			}
			clan.add(elephant_matriarch);
			newGenerationClans.add(clan);
		}

		// Determine Fitness
		calculateFitnessAndSortClans(newGenerationClans);

		// male replacement
		for (int i = 0; i < Properties.NUMBER_OF_ELEPHANT_CLANS; i++) {
			// Get rid of N males
			List<T> newClan = new ArrayList<>(newGenerationClans.get(i).subList(0,
					newGenerationClans.get(i).size() - Properties.NUMBER_OF_MALE_ELEPHANTS_PER_CLAN));
			
			newGenerationClans.set(i, newClan);

			// Add new N males, either from a chromosomeFactory or from the archive
			for (int j = 0; j < Properties.NUMBER_OF_MALE_ELEPHANTS_PER_CLAN; j++) {
				T newElephant;

				// Get new male
				if (!Archive.getArchiveInstance().isArchiveEmpty()
						&& (Properties.SELECT_NEW_ELEPHANTS_FROM_ARCHIVE || Randomness.nextBoolean())) {
					newElephant = (T) generateSuiteFromArchive();
					newElephant.mutate();
				} else {
					newElephant = chromosomeFactory.getChromosome();
				}
				// assert newElephant != null; necessary?

				// In case new male has changed since last evaluation, re-evaluate it
				if (newElephant.isChanged()) {
					fitnessFunctions.forEach(newElephant::addFitness);
					newElephant.updateAge(currentIteration);
					calculateFitness(newElephant);
				}

				newGenerationClans.get(i).add(newElephant);
			}
		}

		// join clans to form population
		for (List<T> c : newGenerationClans) {
			newGeneration.addAll(c);
		}

		population = newGeneration;
		clans = newGenerationClans;
		// archive
		updateFitnessFunctionsAndValues();
		//
		currentIteration++;
	}

	private TestSuiteChromosome generateSuiteFromArchive() {
		// Need to check the Properties.STRATEGY?
		TestSuiteChromosome suite = new TestSuiteChromosome(new ArchiveTestChromosomeFactory());

		// Deactivate in case a test is executed and would access the archive as this
		// might cause a
		// concurrent access.

		// Properties.TEST_ARCHIVE = false;

		for (TestChromosome test : Archive.getArchiveInstance().getSolutions()) {
			if (Randomness.nextBoolean()) {
				suite.addTest(test.clone());
			} else {
				if (suite.size() == 0) { // to avoid empty suite
					suite.addTest(test.clone());
				}
			}
			if (suite.size() == Properties.MAX_SIZE) {
				break;
			}
		}

		// re-active it

		// Properties.TEST_ARCHIVE = true;

		// The archive may contain tests evaluated with a fitness function
		// that is not part of the optimization (e.g. ibranch secondary objective)
		suite.getCoverageValues().keySet().removeIf(ff -> !fitnessFunctions.contains(ff));
		return suite;
	}

	/** {@inheritDoc} */
	@Override
	public void initializePopulation() {
		notifySearchStarted();
		currentIteration = 0;

		// Set up initial population
		generateInitialPopulation(Properties.POPULATION);

		// Set up elephant clans
		for (int i = 0; i < Properties.NUMBER_OF_ELEPHANT_CLANS; i++) {
			int index_start = i * population.size() / Properties.NUMBER_OF_ELEPHANT_CLANS;
			int index_end = (i + 1) * (population.size()) / Properties.NUMBER_OF_ELEPHANT_CLANS;

			List<T> clan = new ArrayList<>();
			for (int j = index_start; j < index_end; j++) {
				clan.add(population.get(j));
			}
			clans.add(clan);
		}
		// Determine fitness
		calculateFitnessAndSortPopulation();
		sortClans(clans);
		this.notifyIteration();
	}

	/** {@inheritDoc} */
	@Override
	public void generateSolution() {
		if (Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER > 0 || Properties.ENABLE_SECONDARY_OBJECTIVE_STARVATION) {
			disableFirstSecondaryCriterion();
		}
		if (population.isEmpty()) {
			initializePopulation();
		}

		logger.debug("Starting evolution");
		int starvationCounter = 0;
		double bestFitness = Double.MAX_VALUE;
		double lastBestFitness = Double.MAX_VALUE;
		if (getFitnessFunction().isMaximizationFunction()) {
			bestFitness = 0.0;
			lastBestFitness = 0.0;
		}
		while (!isFinished()) {
			logger.debug("Current population: " + getAge() + "/" + Properties.SEARCH_BUDGET);
			logger.info("Best fitness: " + getBestIndividual().getFitness());
			evolve();

			sortPopulation();
			sortClans(clans);

			// Local Search
			// applyLocalSearch();

//			double newFitness = getBestIndividual().getFitness();
//
//			if (getFitnessFunction().isMaximizationFunction())
//				assert (newFitness >= bestFitness)
//						: "best fitness was: " + bestFitness + ", now best fitness is " + newFitness;
//			else
//				assert (newFitness <= bestFitness)
//						: "best fitness was: " + bestFitness + ", now best fitness is " + newFitness;
//			bestFitness = newFitness;

			if (Double.compare(bestFitness, lastBestFitness) == 0) {
				starvationCounter++;
			} else {
				logger.info("reset starvationCounter after " + starvationCounter + " iterations");
				starvationCounter = 0;
				lastBestFitness = bestFitness;

			}

			updateSecondaryCriterion(starvationCounter);

			this.notifyIteration();
		}
		TimeController.execute(this::updateBestIndividualFromArchive, "update from archive", 5_000);
		notifySearchFinished();
	}
}