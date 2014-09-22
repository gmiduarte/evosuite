/**
 * Copyright (C) 2011,2012 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Public License for more details.
 * 
 * You should have received a copy of the GNU Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.method;

import com.examples.with.different.packagename.*;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.Properties.Criterion;
import org.evosuite.SystemTest;
import org.evosuite.TestSuiteGenerator;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Jose Miguel Rojas
 *
 */
public class TestMethodNoExceptionCoverageFitnessFunction extends SystemTest {

	@Before
	public void beforeTest() {
        Properties.CRITERION[0] = Criterion.METHODNOEXCEPTION;
		Properties.MINIMIZE = false;
	}

	@Ignore
	public void testMethodNoExceptionFitnessOnlyExceptionExample() {
		EvoSuite evosuite = new EvoSuite();
		
		String targetClass = OnlyException.class.getCanonicalName();
		Properties.TARGET_CLASS = targetClass;
		
		String[] command = new String[] { "-generateSuite", "-class", targetClass };
		Object result = evosuite.parseCommandLine(command);
		GeneticAlgorithm<?> ga = getGAFromResult(result);
		TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();

		System.out.println("EvolvedTestSuite:\n" + best);
		int goals = TestSuiteGenerator.getFitnessFactory().get(0).getCoverageGoals().size(); // assuming single fitness function
		Assert.assertEquals(2, goals );
		Assert.assertEquals("Not expected coverage: ", 0.5d, best.getCoverage(), 0.001);
	}

    @Ignore
    public void testMethodNoExceptionFitnessUnlikelyNoExceptionExample() {
        EvoSuite evosuite = new EvoSuite();

        String targetClass = UnlikelyNoException.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        Properties.DYNAMIC_SEEDING = true;

        String[] command = new String[] { "-generateSuite", "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();

        System.out.println("EvolvedTestSuite:\n" + best);
        int goals = TestSuiteGenerator.getFitnessFactory().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals(2, goals );
        Assert.assertEquals("Non-optimal coverage: ", 1d, best.getCoverage(), 0.001);
    }

    @Test
    public void testMethodNoExceptionFitnessImplicitExplicitExample() {
        EvoSuite evosuite = new EvoSuite();

        String targetClass = ImplicitExplicitException.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        String[] command = new String[] { "-generateSuite", "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();

        System.out.println("EvolvedTestSuite:\n" + best);
        int goals = TestSuiteGenerator.getFitnessFactory().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals(6, goals );
        Assert.assertEquals("Not expected coverage: ", 0.83d, best.getCoverage(), 0.1);
    }
}