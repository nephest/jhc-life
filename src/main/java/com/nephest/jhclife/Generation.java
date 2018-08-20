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

import java.nio.ByteBuffer;
import java.util.*;

public class Generation
{

    public static final byte[] MAGIC_BYTES = new byte[]{0xE, 0xA, 0xE, 0x1};

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

    //supports Integer.MAX_VALUE population max
    public static byte[] toByteArray(Generation generation)
    {
        checkGenerationDimensions(generation.getWidth(), generation.getHeight());

        int size =
            MAGIC_BYTES.length
            + 8 //id
            + 8 //generationNumber
            + 4 //width
            + 4 //height
            + (int) Math.ceil( (generation.getWidth() * generation.getHeight()) / 8.0d ); //population

        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.put(MAGIC_BYTES, 0, MAGIC_BYTES.length);
        buf.putLong(generation.getId());
        buf.putLong(generation.getGenerationNumber());
        buf.putInt(generation.getWidth());
        buf.putInt(generation.getHeight());

        BitSet bits = new BitSet(generation.getWidth() * generation.getHeight());
        int ix = 0;
        for (int col = 0; col < generation.getWidth(); col++)
        {
            for (int row = 0; row < generation.getHeight(); row++, ix++)
            {
                if (generation.isPopulationAlive(col, row)) bits.set(ix);
            }
        }

        byte[] population = bits.toByteArray();
        buf.put(population, 0, population.length);
        buf.flip();
        byte[] result = new byte[buf.limit()];
        buf.get(result);
        return result;
    }

    //supports Integer.MAX_VALUE population max
    public static Generation fromByteArray(byte[] bytes)
    {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int i = 0; i < MAGIC_BYTES.length; i++)
        {
            if (buf.get() != MAGIC_BYTES[i])
                throw new IllegalArgumentException("Invalid magic header");
        }
        long id = buf.getLong();
        long generationNumber = buf.getLong();
        int width = buf.getInt();
        int height = buf.getInt();
        checkGenerationDimensions(width, height);
        boolean[][] population = new boolean[width][height];

        byte[] populationBytes = new byte[buf.remaining()];
        buf.get(populationBytes);
        BitSet bits = BitSet.valueOf(populationBytes);

        int ix = 0;
        for (int col = 0; col < width; col++)
        {
            for (int row = 0; row < height; row++, ix++)
            {
                if (bits.get(ix)) population[col][row] = true;
            }
        }

        return new Generation(population, id, generationNumber);
    }

    private static final void checkGenerationDimensions(int width, int height)
    {
        try
        {
            Math.multiplyExact(width, height);
        }
        catch (ArithmeticException ex)
        {
            throw new IllegalArgumentException
            ("Generaitons with more than Integer.MAX_VALUE population count are not supported");
        }
    }

    public boolean isPopulationAlive(int x, int y)
    {
        if (x < 0 || x > getWidth())
            throw new IllegalArgumentException("x out of bounds");
        if (y < 0 || y > getHeight())
            throw new IllegalArgumentException("y out of bounds");
        return this.population[x][y];
    }

    public boolean[][] copyPopulation()
    {
        boolean[][] copy = new boolean[getWidth()][getHeight()];
        for(int i = 0; i < this.population.length; i++)
        {
            copy[i]
                = Arrays.copyOf(this.population[i], this.population[i].length);
        }
        return copy;
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
