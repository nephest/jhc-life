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

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassicLifeModel
implements java.io.Closeable
{

    private static final Logger LOG
        = Logger.getLogger(ClassicLifeModel.class.getName());

    public static final int POPULATION_MIN = 2;
    public static final int POPULATION_MAX = 3;
    public static final int POPULATION_REPRODUCTION = 3;

    private final ForkJoinPool forkJoinPool;
    private final ScheduledExecutorService executor;
    private final Random random = new Random();
    private GenerationCalculator generationCalculator;
    private ScheduledFuture generationFuture;

    private int width;
    private int height;
    private boolean[][] population;
    private boolean[][] lastPopulation;
    private Generation lastGeneration;
    private double populationProbability = 0.5;
    private long generation = 0;
    private long id = 0;

    private volatile boolean closed = false;
    private boolean externalExecutor = true;
    private volatile boolean running = false;
    private long generationLifeTimePeriod = 1;
    private TimeUnit generationLifeTimeUnit = TimeUnit.SECONDS;
    private long lastGenerationNanos = System.nanoTime();

    public ClassicLifeModel
    (
        int width,
        int height,
        ForkJoinPool pool,
        ScheduledExecutorService executor
    )
    {
        Objects.requireNonNull(executor);
        this.width = width;
        this.height = height;
        createNewPopulation(width, height);
        this.forkJoinPool = pool;
        this.executor = executor;
    }

    public ClassicLifeModel(int width, int height)
    {
        this
        (
            width,
            height,
            null,
            Executors.newSingleThreadScheduledExecutor
            (
                (r)->
                {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(false);
                    t.setName("ClassicLifeModel manager");
                    return t;
                }
            )
        );
        this.externalExecutor = false;
    }

    public synchronized void createNewPopulation(int width, int height)
    {
        if (width < 0) throw new IllegalArgumentException("width must be more than 0");
        if (height < 0) throw new IllegalArgumentException("height must be more than 0");
        boolean wasRunning = isRunning();
        if (wasRunning) stop();
        setPopulationDimensions(width, height);
        savePopulation();
        resetGenerationNumber();
        externalModification();
        if (wasRunning) start();
    }

    private void setPopulationDimensions(int width, int height)
    {
        this.width = width;
        this.height = height;
        this.population = new boolean[width][height];
        this.lastPopulation = new boolean[width][height];
    }

    @Override
    public void close()
    {
        if (isClosed()) return;
        stop();
        if (!isExternalExecutor()) getExecutor().shutdown();
        this.closed = true;
    }

    public boolean isClosed()
    {
        return this.closed;
    }

    private ForkJoinPool getForkJoinPool()
    {
        return this.forkJoinPool;
    }

    protected ScheduledExecutorService getExecutor()
    {
        return this.executor;
    }

    private Random getRandom()
    {
        return this.random;
    }

    private GenerationCalculator getGenerationCalculator()
    {
        return this.generationCalculator;
    }

    private ScheduledFuture getGenerationFuture()
    {
        return this.generationFuture;
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    protected boolean[][] getPopulation()
    {
        return this.population;
    }

    protected boolean[][] getLastPopulation()
    {
        return this.lastPopulation;
    }

    private void setPopulationProbability(double probability)
    {
        this.populationProbability = probability;
    }

    public double getPopulationProbability()
    {
        return this.populationProbability;
    }

    public long getGenerationNumber()
    {
        return this.generation;
    }

    private void resetGenerationNumber()
    {
        this.generation = 0;
    }

    public long getId()
    {
        return this.id;
    }

    private boolean isExternalExecutor()
    {
        return this.externalExecutor;
    }

    public boolean isRunning()
    {
        return this.running;
    }

    public synchronized void start()
    {
        if (isRunning()) return;
        if (isClosed())
            throw new IllegalStateException("Can't start model. Resources are closed");
        this.generationFuture = getExecutor().scheduleAtFixedRate
        (
            this::nextGeneration,
            calculateFinalDelay(),
            this.generationLifeTimePeriod,
            this.generationLifeTimeUnit
        );
        this.running = true;
    }

    private long calculateFinalDelay()
    {
        long delta = System.nanoTime() - getLastGenerationNanos();
        return this.generationLifeTimePeriod
            - this.generationLifeTimeUnit.convert(delta, TimeUnit.NANOSECONDS);
    }

    public synchronized void stop()
    {
        if(!isRunning()) return;
        getGenerationFuture().cancel(false);
        if(getGenerationCalculator() != null)
            getGenerationCalculator().cancel(false);
        boolean got = getGenerationFuture().isCancelled();
        while (!got)
        {
            try
            {
                getGenerationFuture().get();
                got = true;
            }
            catch (InterruptedException ex)
            {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
            catch (ExecutionException ex)
            {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                got = true;
            }
        }
        this.running = false;
    }

    public long getGenerationLifeTime(TimeUnit unit)
    {
        return unit.convert(this.generationLifeTimePeriod, this.generationLifeTimeUnit);
    }

    public synchronized void setGenerationLifeTime(long period, TimeUnit unit)
    {
        boolean wasRunning = isRunning();
        if (wasRunning) stop();
        this.generationLifeTimePeriod = period;
        this.generationLifeTimeUnit = unit;
        if (wasRunning) start();
    }

    private void externalModification()
    {
        this.id++;
        saveGeneration();
    }

    public synchronized void populate(long seed, double populationProbability)
    {
        if (populationProbability < 0 || populationProbability > 1.0)
            throw new IllegalArgumentException("population probability must be in 0-1 range");
        boolean wasRunning = isRunning();
        if (wasRunning) stop();
        getRandom().setSeed(seed);
        setPopulationProbability(populationProbability);
        for (int col = 0; col < getPopulation().length; col++)
        {
            for (int row = 0; row < getPopulation()[col].length; row++)
            {
                getPopulation()[col][row] = nextPopulation();
            }
        }
        savePopulation();
        resetGenerationNumber();
        externalModification();
        if (wasRunning) start();
    }

    private void savePopulation()
    {
        for(int i = 0; i < getPopulation().length; i++)
        {
            getLastPopulation()[i]
                = Arrays.copyOf(getPopulation()[i], getPopulation()[i].length);
        }
    }

    private boolean[][] copyLastPopulation()
    {
        boolean[][] copy = new boolean[getWidth()][getHeight()];
        for(int i = 0; i < getLastPopulation().length; i++)
        {
            copy[i]
                = Arrays.copyOf(getLastPopulation()[i], getLastPopulation()[i].length);
        }
        return copy;
    }

    protected boolean nextPopulation()
    {
        return getRandom().nextDouble() < getPopulationProbability();
    }

    protected void nextGeneration()
    {
        this.generationCalculator
            = new GenerationCalculator(getLastPopulation(), getPopulation());
        ForkJoinPool pool
            = getForkJoinPool() == null
            ? ForkJoinPool.commonPool()
            : getForkJoinPool();
        pool.invoke(getGenerationCalculator());
        savePopulation();
        this.generation++;
        saveGeneration();
        this.lastGenerationNanos = System.nanoTime();
    }

    public synchronized void setPopulation(int x, int y, boolean pop)
    {
        if (x < 0 || x > getWidth())
            throw new IllegalArgumentException("x out of bounds");
        if (y < 0 || y > getHeight())
            throw new IllegalArgumentException("y out of bounds");
        boolean wasRunning = isRunning();
        if (wasRunning) stop();
        getLastPopulation()[x][y] = pop;
        externalModification();
        if (wasRunning) start();
    }

    public synchronized void setGeneration(Generation generation)
    {
        boolean wasRunning = isRunning();
        if (wasRunning) stop();

        setPopulationDimensions(generation.getWidth(), generation.getHeight());
        this.population = generation.copyPopulation();
        this.generation = generation.getGenerationNumber();
        savePopulation();
        externalModification();

        if (wasRunning) start();
    }

    private void saveGeneration()
    {
        if
        (
            this.lastGeneration == null
            || this.lastGeneration.getId() != getId()
            || this.lastGeneration.getGenerationNumber() != getGenerationNumber()
        )
        {
            this.lastGeneration = new Generation
            (
                copyLastPopulation(),
                getId(),
                getGenerationNumber()
            );
        }
    }

    public Generation getLastGeneration()
    {
        saveGeneration();
        return this.lastGeneration;
    }

    public long getLastGenerationNanos()
    {
        return this.lastGenerationNanos;
    }

}
