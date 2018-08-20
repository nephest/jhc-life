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

import org.junit.*;
import static org.junit.Assert.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

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

}
