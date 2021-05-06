/*-
 * =========================LICENSE_START=========================
 * jhc-life
 * %%
 * Copyright (C) 2018 - 2021 Oleksandr Masniuk
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * =========================LICENSE_END=========================
 */

package com.nephest.jhclife;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.LessOrEqual;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClassicLifeModelTest
{

    public static final int MODEL_WIDTH = 800;
    public static final int MODEL_HEIGHT = 600;

    private final Random rng = new Random();
    private ClassicLifeModel model;
    private ScheduledExecutorService executorMock;
    private ScheduledFuture generationFutureMock;

    @BeforeEach
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
            int[][] first = deepCopy(this.model.getPopulation());
            assertTrue(Arrays.deepEquals(first, this.model.getLastPopulation()));
            assertTrue(this.model.getId() != lastId);

            lastId = this.model.getId();
            this.model.populate(seed, prob);
            int[][] second = deepCopy(this.model.getPopulation());
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
            String
                sb =
                "population probability threshold exceeded. expected:"
                    + threshold
                    + " got: "
                    + diff
                    + ", expected probability: "
                    + prob
                    + ", actual probability: "
                    + probActual
                    + ", pop count: "
                    + popCount
                    + "/"
                    + popMax;
            fail(sb);
        }
    }

    @Test
    public void testGeneration()
    {
        this.model.populate(this.rng.nextLong(), 0.5);
        int[][] pop = this.model.getLastPopulation();
        Generation gen = this.model.getLastGeneration();
        for (int col = 0; col < pop.length; col++)
        {
            for(int row = 0; row < pop[col].length; row++)
            {
                assertEquals(pop[col][row] == 1, gen.isPopulationAlive(col, row));
            }
        }
    }

    @Test
    public void testNumerousGenerations()
    {
        int depth = this.rng.nextInt(50);
        long seed = this.rng.nextLong();
        double prob = 0.5;
        int width = 300 + this.rng.nextInt(50);
        int height = 300 + this.rng.nextInt(50);

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
            longThat(new LessOrEqual<>(count)),
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
            longThat(new LessOrEqual<>(newCount)),
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

        int[][] pop = new int[width][height];
        pop[0][0] = 1;
        pop[width - 1][height - 1] = 1;
        pop[width / 2][height / 2] = 1;

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

    private int[][] deepCopy(int[][] src)
    {
        int[][] result = new int[src.length][];
        for (int i = 0; i < src.length; i++)
        {
            result[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return result;
    }

    private int calculatePopCount(int[][] pops)
    {
        int count = 0;
        for (int[] col : pops)
        {
            for (int pop : col)
            {
                count+=pop;
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
            longThat(new LessOrEqual<>(count)),
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
