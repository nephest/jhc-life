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

public class ClassicLifeModel
{

    public static final int POPULATION_MIN = 2;
    public static final int POPULATION_MAX = 3;
    public static final int POPULATION_REPRODUCTION = 3;

    private final ForkJoinPool forkJoinPool;
    private final Random random = new Random();

    private int width;
    private int height;
    private boolean[][] population;
    private boolean[][] lastPopulation;
    private Generation lastGeneration;
    private double populationProbability = 0.5;
    private long generation = 0;
    private long id = 0;

    public ClassicLifeModel(int width, int height, ForkJoinPool pool)
    {
        this.width = width;
        this.height = height;
        createNewPopulation(width, height);
        this.forkJoinPool = pool;
    }

    public ClassicLifeModel(int width, int height)
    {
        this(width, height, null);
    }

    public final void createNewPopulation(int width, int height)
    {
        if (width < 0) throw new IllegalArgumentException("width must be more than 0");
        if (height < 0) throw new IllegalArgumentException("height must be more than 0");
        this.width = width;
        this.height = height;
        this.population = new boolean[width][height];
        this.lastPopulation = new boolean[width][height];
        savePopulation();
        resetGenerationNumber();
        externalModification();
    }

    private ForkJoinPool getForkJoinPool()
    {
        return this.forkJoinPool;
    }

    private Random getRandom()
    {
        return this.random;
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

    private void externalModification()
    {
        this.id++;
    }

    public void populate(long seed, double populationProbability)
    {
        if (populationProbability < 0 || populationProbability > 1.0)
            throw new IllegalArgumentException("population probability must be in 0-1 range");
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

    public void nextGeneration()
    {
        GenerationCalculator calc
            = new GenerationCalculator(getLastPopulation(), getPopulation());
        ForkJoinPool pool
            = getForkJoinPool() == null
            ? ForkJoinPool.commonPool()
            : getForkJoinPool();
        pool.invoke(calc);
        savePopulation();
        this.generation++;
    }

    public void setPopulation(int x, int y, boolean pop)
    {
        if (x < 0 || x > getWidth())
            throw new IllegalArgumentException("x out of bounds");
        if (y < 0 || y > getHeight())
            throw new IllegalArgumentException("y out of bounds");
        getLastPopulation()[x][y] = pop;
        externalModification();
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

}
