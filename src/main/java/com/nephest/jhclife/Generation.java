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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;

public class Generation
{

    public static final byte[] MAGIC_BYTES = new byte[]{0xE, 0xA, 0xE, 0x1};

    private final int[][] population;
    private final long id;
    private final long generationNumber;
    private final long populationCount;
    private final int width;
    private final int height;

    public Generation
    (
        int[][] population,
        long id,
        long generationNumber
    )
    {
        this.population = population;
        this.id = id;
        this.generationNumber = generationNumber;
        this.populationCount = countPopulation(population);
        this.width = Math.max(population.length, 0);
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
        for (byte magicByte : MAGIC_BYTES)
        {
            if (buf.get() != magicByte) throw new IllegalArgumentException("Invalid magic header");
        }
        long id = buf.getLong();
        long generationNumber = buf.getLong();
        int width = buf.getInt();
        int height = buf.getInt();
        checkGenerationDimensions(width, height);
        int[][] population = new int[width][height];

        byte[] populationBytes = new byte[buf.remaining()];
        buf.get(populationBytes);
        BitSet bits = BitSet.valueOf(populationBytes);

        int ix = 0;
        for (int col = 0; col < width; col++)
        {
            for (int row = 0; row < height; row++, ix++)
            {
                if (bits.get(ix)) population[col][row] = 1;
            }
        }

        return new Generation(population, id, generationNumber);
    }

    public static long countPopulation(int[][] population)
    {
        long[] counts = new long[population.length];
        IntStream.range(0, population.length).boxed()
            .parallel()
            .forEach(i->{for(int pop : population[i] ) counts[i] += pop;});
        return Arrays.stream(counts).sum();
    }

    private static void checkGenerationDimensions(int width, int height)
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
        return this.population[x][y] == 1;
    }

    public int[][] copyPopulation()
    {
        int[][] copy = new int[getWidth()][getHeight()];
        for(int i = 0; i < this.population.length; i++)
        {
            copy[i]
                = Arrays.copyOf(this.population[i], this.population[i].length);
        }
        return copy;
    }

    public int[] copyPopulation1D()
    {
        int[][] b2d = population;
        int[] b1d = new int[b2d.length * b2d[0].length];
        int k = 0;
        for(int y = 0; y < b2d[0].length; y++)
            for(int x = 0; x < b2d.length; x++)
                b1d[k++] = b2d[x][y];
        return b1d;
    }

    public long getId()
    {
        return this.id;
    }

    public long getGenerationNumber()
    {
        return this.generationNumber;
    }

    public long getPopulationCount()
    {
        return populationCount;
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
