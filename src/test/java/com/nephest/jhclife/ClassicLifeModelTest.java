/*-
 * =========================LICENSE_START=========================
 * jhc-life
 * %%
 * Copyright (C) 2018 Oleksandr Masniuk
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * =========================LICENSE_END=========================
 */

package com.nephest.jhclife;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.mockito.*;
import org.mockito.internal.matchers.*;
import static org.mockito.Mockito.*;

public class ClassicLifeModelTest
{

    public static final int MODEL_WIDTH = 800;
    public static final int MODEL_HEIGHT = 600;

    private final Random rng = new Random();
    private ClassicLifeModel model;
    private ScheduledExecutorService executorMock;
    private ScheduledFuture generationFutureMock;

    @Before
    public void init()
    {
        this.executorMock = mock(ScheduledExecutorService.class);
        this.generationFutureMock = mock(ScheduledFuture.class);
        when(this.executorMock.scheduleAtFixedRate(any(), anyLong(), anyLong(), any()))
            .thenReturn(this.generationFutureMock);
        model = new ClassicLifeModel
        (
            MODEL_WIDTH,
            MODEL_HEIGHT,
            null,
            executorMock
        );
    }

    @Test
    public void testConstructor()
    {
        assertEquals(MODEL_WIDTH, this.model.getWidth());
        assertEquals(MODEL_WIDTH, this.model.getPopulation().length);
        assertEquals(MODEL_WIDTH, this.model.getLastGeneration().getWidth());
        assertEquals(MODEL_HEIGHT, this.model.getHeight());
        assertEquals(MODEL_HEIGHT, this.model.getPopulation()[0].length);
        assertEquals(MODEL_HEIGHT, this.model.getLastGeneration().getHeight());
    }

    @Test
    public void testNewPopulation()
    {
        int width = 1;
        int height = 2;
        long lastId = this.model.getId();
        this.model.createNewPopulation(width, height);
        assertEquals(width, this.model.getWidth());
        assertEquals(width, this.model.getPopulation().length);
        assertEquals(width, this.model.getLastGeneration().getWidth());
        assertEquals(height, this.model.getHeight());
        assertEquals(height, this.model.getPopulation()[0].length);
        assertEquals(height, this.model.getLastGeneration().getHeight());
        assertTrue(this.model.getId() != lastId);
    }

    @Test
    public void testSeedPopulating()
    {
        int cycles = 10;
        long lastId = this.model.getId();
        for(int i = 0; i < cycles; i++)
        {
            long seed = this.rng.nextLong();
            double prob = this.rng.nextDouble();

            lastId = this.model.getId();
            this.model.populate(seed, prob);
            boolean[][] first = deepCopy(this.model.getPopulation());
            assertTrue(Arrays.deepEquals(first, this.model.getLastPopulation()));
            assertTrue(this.model.getId() != lastId);

            lastId = this.model.getId();
            this.model.populate(seed, prob);
            boolean[][] second = deepCopy(this.model.getPopulation());
            assertTrue(Arrays.deepEquals(second, this.model.getLastPopulation()));
            assertTrue(this.model.getId() != lastId);

            assertTrue(Arrays.deepEquals(first, second));
        }
    }

    @Test
    public void testPopulatingProbability()
    {
        testPopulatingProbability(0.0, 0.0);
        testPopulatingProbability(1.0, 0.0);
        int cycles = 10;
        double threshold = 0.05;
        for(int i = 0; i < cycles; i++)
        {
            testPopulatingProbability(rng.nextDouble(), threshold);
        }
    }

    private void testPopulatingProbability(double prob, double threshold)
    {
        this.model.populate(this.rng.nextLong(), prob);
        int popCount = calculatePopCount(this.model.getPopulation());
        int popMax = this.model.getWidth() * this.model.getHeight();
        double probActual = popCount / (double) popMax;
        double diff = Math.abs(prob - probActual);
        if (diff > threshold)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("population probability threshold exceeded. expected:")
            .append(threshold)
            .append(" got: ").append(diff)
            .append(", expected probability: ").append(prob)
            .append(", actual probability: ").append(probActual)
            .append(", pop count: ").append(popCount).append("/").append(popMax);
            fail(sb.toString());
        }
    }

    @Test
    public void testGeneration()
    {
        this.model.populate(this.rng.nextLong(), 0.5);
        boolean[][] pop = this.model.getLastPopulation();
        Generation gen = this.model.getLastGeneration();
        for (int col = 0; col < pop.length; col++)
        {
            for(int row = 0; row < pop[col].length; row++)
            {
                assertEquals(pop[col][row], gen.isPopulationAlive(col, row));
            }
        }
    }

    @Test
    public void testNumerousGenerations()
    {
        int depth = this.rng.nextInt(250);
        long seed = this.rng.nextLong();
        double prob = 0.5;
        int width = 500 + this.rng.nextInt(50);
        int height = 500 + this.rng.nextInt(50);

        this.model.createNewPopulation(width, height);
        this.model.populate(seed, prob);
        for (int i = 0; i < depth; i++)
        {
            this.model.nextGeneration();
        }
        Generation first = this.model.getLastGeneration();

        this.model.createNewPopulation(width, height);
        this.model.populate(seed, prob);
        for (int i = 0; i < depth; i++)
        {
            this.model.nextGeneration();
        }
        Generation second = this.model.getLastGeneration();

        for (int col = 0; col < first.getWidth(); col++)
        {
            for (int row = 0; row < first.getHeight(); row++)
            {
                assertEquals
                (
                    first.isPopulationAlive(col, row),
                    second.isPopulationAlive(col, row)
                );
            }
        }
    }

    @Test
    public void testStart()
    {
        assertFalse(this.model.isRunning());
        nextGeneration();
        assertTrue(this.model.isRunning());
    }

    @Test
    public void testStop()
    throws InterruptedException, ExecutionException
    {
        assertFalse(this.model.isRunning());
        nextGeneration();
        assertTrue(this.model.isRunning());
        this.model.stop();
        assertFalse(this.model.isRunning());
        verify(this.generationFutureMock).cancel(false);
        verify(this.generationFutureMock).get();
    }

    @Test
    public void testExternalExecutorClose()
    {
        assertFalse(this.model.isClosed());
        this.model.close();
        assertFalse(this.model.isRunning());
        assertTrue(this.model.isClosed());
        verify(this.executorMock, never()).shutdown();
    }

    @Test
    public void testInternalExecutorClose()
    {
        this.model = new ClassicLifeModel(MODEL_WIDTH, MODEL_HEIGHT);
        assertFalse(this.model.isClosed());
        this.model.close();
        assertFalse(this.model.isRunning());
        assertTrue(this.model.isClosed());
        assertTrue(this.model.getExecutor().isShutdown());
    }

    @Test
    public void testSetGenerationLifeTime()
    throws InterruptedException, ExecutionException
    {
        TimeUnit unit = TimeUnit.SECONDS;
        long count = 10;

        //do not start the model if it was not running
        assertFalse(this.model.isRunning());
        this.model.setGenerationLifeTime(count, unit);
        assertFalse(this.model.isRunning());
        assertEquals(count, this.model.getGenerationLifeTime(unit));
        verify(executorMock, never())
            .scheduleAtFixedRate(any(), eq(count), eq(count), eq(unit));

        //normal start with updated parameters
        this.model.start();
        verify(executorMock).scheduleAtFixedRate
        (
            any(),
            longThat(new LessOrEqual(count)),
            eq(count),
            eq(unit)
        );

        //auto restart with the new life time if the model was already running
        TimeUnit newUnit = TimeUnit.HOURS;
        long newCount = 22;
        assertTrue(this.model.isRunning());
        this.model.setGenerationLifeTime(newCount, newUnit);
        assertTrue(this.model.isRunning());
        assertEquals(newCount, this.model.getGenerationLifeTime(newUnit));
        verify(executorMock).scheduleAtFixedRate
        (
            any(),
            longThat(new LessOrEqual(newCount)),
            eq(newCount),
            eq(newUnit)
        );
        //verify that previous tasks were canceled gracefully on restart
        verify(this.generationFutureMock).cancel(false);
        verify(this.generationFutureMock).get();
    }

    @Test
    public void testGenerationNumberIncrement()
    {
        assertEquals(0, this.model.getGenerationNumber());
        assertEquals(0, this.model.getLastGeneration().getGenerationNumber());
        nextGeneration();
        assertEquals(1, this.model.getGenerationNumber());
        assertEquals(1, this.model.getLastGeneration().getGenerationNumber());
    }

    @Test
    public void testGenerationNumberReset()
    {
        assertEquals(0, this.model.getGenerationNumber());
        assertEquals(0, this.model.getLastGeneration().getGenerationNumber());

        nextGeneration();
        this.model.populate(this.rng.nextLong(), 0.5);
        assertEquals(0, this.model.getGenerationNumber());
        assertEquals(0, this.model.getLastGeneration().getGenerationNumber());

        nextGeneration(3); //generation, population, generation
        this.model.createNewPopulation(MODEL_WIDTH, MODEL_HEIGHT);
        assertEquals(0, this.model.getGenerationNumber());
        assertEquals(0, this.model.getLastGeneration().getGenerationNumber());

    }

    @Test
    public void testSetAlivePopulation()
    {
        this.model.populate(rng.nextLong(), 0);
        long lastId = this.model.getId();
        this.model.setPopulation(1, 1, true);
        assertTrue(this.model.getLastGeneration().isPopulationAlive(1, 1));
        assertTrue(this.model.getId() != lastId);
    }

    @Test
    public void testSetDeadPopulation()
    {
        this.model.populate(rng.nextLong(), 1);
        long lastId = this.model.getId();
        this.model.setPopulation(1, 1, false);
        assertFalse(this.model.getLastGeneration().isPopulationAlive(1, 1));
        assertTrue(this.model.getId() != lastId);
    }

    @Test
    public void testSetGeneration()
    {
        long lastId = this.model.getId();
        int width = 142;
        int height = 187;
        long id = 80312;
        long generationNumber = 6421890;

        boolean[][] pop = new boolean[width][height];
        pop[0][0] = true;
        pop[width - 1][height - 1] = true;
        pop[width / 2][height / 2] = true;

        Generation gen = mock(Generation.class);
        when(gen.getWidth()).thenReturn(width);
        when(gen.getHeight()).thenReturn(height);
        when(gen.getId()).thenReturn(id);
        when(gen.getGenerationNumber()).thenReturn(generationNumber);
        when(gen.copyPopulation()).thenReturn(pop);

        this.model.setGeneration(gen);
        assertEquals(width, this.model.getWidth());
        assertEquals(height, this.model.getHeight());
        assertNotEquals(lastId, this.model.getId());
        assertEquals(generationNumber, this.model.getGenerationNumber());
        assertTrue(Arrays.deepEquals(pop, this.model.getPopulation()));
        assertTrue(Arrays.deepEquals(pop, this.model.getLastPopulation()));
        assertTrue(Arrays.deepEquals(pop, this.model.getLastGeneration().copyPopulation()));
    }

    @Test
    public void testOverpopulaiton()
    {
        this.model.populate(rng.nextLong(), 0);
        this.model.setPopulation(0, 0, true);
        this.model.setPopulation(0, 1, true);
        this.model.setPopulation(2, 0, true);
        this.model.setPopulation(1, 2, true);
        this.model.setPopulation(1, 1, true);
        nextGeneration();
        assertFalse(this.model.getLastGeneration().isPopulationAlive(1, 1));
    }

    @Test
    public void testPopulationNormalThree()
    {
        this.model.populate(rng.nextLong(), 0);
        this.model.setPopulation(0, 0, true);
        this.model.setPopulation(2, 0, true);
        this.model.setPopulation(1, 2, true);
        this.model.setPopulation(1, 1, true);
        nextGeneration();
        assertTrue(this.model.getLastGeneration().isPopulationAlive(1, 1));
    }

    @Test
    public void testPopulationNormalTwo()
    {
        this.model.populate(rng.nextLong(), 0);
        this.model.setPopulation(2, 0, true);
        this.model.setPopulation(1, 2, true);
        this.model.setPopulation(1, 1, true);
        nextGeneration();
        assertTrue(this.model.getLastGeneration().isPopulationAlive(1, 1));
    }

    @Test
    public void testUnderpopulation()
    {
        this.model.populate(rng.nextLong(), 0);
        this.model.setPopulation(2, 0, true);
        this.model.setPopulation(1, 1, true);
        nextGeneration();
        assertFalse(this.model.getLastGeneration().isPopulationAlive(1, 1));
    }

    private boolean[][] deepCopy(boolean[][] src)
    {
        boolean[][] result = new boolean[src.length][];
        for (int i = 0; i < src.length; i++)
        {
            result[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return result;
    }

    private int calculatePopCount(boolean[][] pops)
    {
        int count = 0;
        for (boolean[] col : pops)
        {
            for (boolean pop : col)
            {
                if (pop) count++;
            }
        }
        return count;
    }

    private void nextGeneration(int times)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        TimeUnit unit = TimeUnit.SECONDS;
        long count = 10;
        this.model.setGenerationLifeTime(count, unit);
        this.model.start();
        verify(this.executorMock, times(times)).scheduleAtFixedRate
        (
            captor.capture(),
            longThat(new LessOrEqual(count)),
            eq(count),
            eq(unit)
        );
        captor.getValue().run();
    }

    private void nextGeneration()
    {
        nextGeneration(1);
    }

}
