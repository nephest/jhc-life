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

import java.util.concurrent.RecursiveAction;

public class GenerationCalculator
extends RecursiveAction
{
    public static final int SURPLUS_MAX = 3;

    private final boolean[][] src, dest;
    private final GenerationCalculator next;

    private final int begin;
    private int end;

    public GenerationCalculator
    (
        boolean[][] src, boolean[][] dest,
        int begin, int end,
        GenerationCalculator next
    )
    {
        if(src.length != dest.length)
            throw new IllegalArgumentException("src and dest have different lengths");
        this.src = src;
        this.dest = dest;
        this.begin = begin;
        this.end = end;
        this.next = next;
    }

    public GenerationCalculator(boolean[][] src, boolean[][] dest)
    {
        this
        (
            src,
            dest,
            0,
            src.length,
            null
        );
    }

    @Override
    protected void compute()
    {
        GenerationCalculator right = null;
        int b = getBeginIx();
        int e = getEndIx();
        while(e - b > 1 && getSurplusQueuedTaskCount() <= SURPLUS_MAX)
        {
            int mid = (b + e) >>> 1;
            right = new GenerationCalculator
            (
                getSource(),
                getDestination(),
                mid,
                e,
                right
            );
            right.fork();
            e = mid;
        }
        calculateNextPopulation();
        while(right != null)
        {
            if (right.tryUnfork())
            {
                right.calculateNextPopulation();
            }
            else
            {
                right.join();
            }
            right = right.getNext();
        }
    }

    private boolean[][] getSource()
    {
        return this.src;
    }

    private boolean[][] getDestination()
    {
        return this.dest;
    }

    private GenerationCalculator getNext()
    {
        return this.next;
    }

    private int getBeginIx()
    {
        return this.begin;
    }

    private int getEndIx()
    {
        return this.end;
    }

    private void setEndIx(int ix)
    {
        this.end = ix;
    }

    private int getWidth()
    {
        return getSource().length;
    }

    private int getHeight()
    {
        return getSource().length < 1
            ? 0
            : getSource()[0].length;
    }

    private void calculateNextPopulation()
    {
        for (int x = getBeginIx(); x < getEndIx(); x++)
        {
            for (int y = 0; y < getSource()[x].length; y++)
            {
                getDestination()[x][y] = willLive(x, y, getSource());
            }
        }
    }

    private boolean willLive(int x, int y, boolean[][] population)
    {
        boolean result = false;
        int neighborCount = calculateNeighborCount(x, y, population);
        if(population[x][y])
        {
            if
            (
                neighborCount >= ClassicLifeModel.POPULATION_MIN
                && neighborCount <= ClassicLifeModel.POPULATION_MAX
            )
                result = true;
        }
        else
        {
            if (neighborCount == ClassicLifeModel.POPULATION_REPRODUCTION)
                result = true;
        }
        return result;
    }

    private int mod(int x, int y)
    {
        int result = x % y;
        if (result < 0) result += y;
        return result;
    }

    private int calculateNeighborCount(int x, int y, boolean[][] population)
    {
        int xLeft = x - 1;
        int yUp = y + 1;
        return calculateNighborCountRow(xLeft, yUp, population, true)
            + calculateNighborCountRow(xLeft, yUp - 1, population, false)
            + calculateNighborCountRow(xLeft, yUp - 2, population, true);
    }

    private int calculateNighborCountRow
    (
        int x, int y,
        boolean[][] population,
        boolean includeMiddle
    )
    {
        int result = 0;
        int mody = mod(y, getHeight());
        for (int i = 0; i < 3; i++)
        {
            if (i == 1 && !includeMiddle) continue;

            int modx = mod(x + i, getWidth());
            if (population[modx][mody]) result++;
        }
        return result;
    }

}
