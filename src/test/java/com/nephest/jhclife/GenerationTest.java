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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenerationTest
{

    @Test
    public void toArrayFromArrayTest()
    {
        int width = 100;
        int height = 100;
        long id = 10;
        long generationNumber = 123;

        boolean[][] pop = new boolean[width][height];
        pop[0][0] = true;
        pop[width - 1][height - 1] = true;
        pop[width / 2][height / 2] = true;

        Generation original = new Generation(pop, id, generationNumber);
        Generation copy = Generation.fromByteArray(Generation.toByteArray(original));

        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getGenerationNumber(), copy.getGenerationNumber());
        assertEquals(original.getWidth(), copy.getWidth());
        assertEquals(original.getHeight(), copy.getHeight());

        for (int col = 0; col < width; col++)
        {
            for (int row = 0; row < height; row++)
            {
                assertEquals
                (
                    original.isPopulationAlive(col, row),
                    copy.isPopulationAlive(col, row)
                );
            }
        }
    }

    @Test
    public void testPopulationCount()
    {
        boolean[][] pop = new boolean[3][3];
        pop[0][0] = true;
        pop[1][1] = true;
        pop[2][2] = true;
        assertEquals(3, Generation.countPopulation(pop));
    }

}
