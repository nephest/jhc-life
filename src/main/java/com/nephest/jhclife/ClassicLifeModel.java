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
import java.util.logging.*;
import java.util.concurrent.*;

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
            ()->nextGeneration(),
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
