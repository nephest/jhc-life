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

public class Generation
{

    private final boolean[][] population;
    private final long id;
    private final long generationNumber;
    private final int width;
    private final int height;

    public Generation
    (
        boolean[][] population,
        long id,
        long generationNumber
    )
    {
        this.population = population;
        this.id = id;
        this.generationNumber = generationNumber;
        this.width = population.length > 0 ? population.length : 0;
        this.height = this.width > 0 ? population[0].length : 0;
    }

    public boolean isPopulationAlive(int x, int y)
    {
        if (x < 0 || x > getWidth())
            throw new IllegalArgumentException("x out of bounds");
        if (y < 0 || y > getHeight())
            throw new IllegalArgumentException("y out of bounds");
        return this.population[x][y];
    }

    public long getId()
    {
        return this.id;
    }

    public long getGenerationNumber()
    {
        return this.generationNumber;
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

}
