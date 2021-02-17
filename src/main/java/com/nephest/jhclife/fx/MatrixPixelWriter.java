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

package com.nephest.jhclife.fx;

import com.nephest.jhclife.Generation;

import java.util.concurrent.*;

import javafx.scene.image.*;
import javafx.scene.paint.Color;

public class MatrixPixelWriter
extends RecursiveAction
{
    public static final int SURPLUS_MAX = 3;
    public static final Color ALIVE_COLOR = Color.BLACK;
    public static final Color DEAD_COLOR = Color.WHITE;

    private final Generation prev;
    private final Generation cur;
    private PixelWriter dest;
    private final MatrixPixelWriter next;
    private final Color aliveColor;
    private final Color deadColor;

    private int begin;
    private int end;

    public MatrixPixelWriter
    (
        Generation prev, Generation cur, PixelWriter dest,
        Color aliveColor, Color deadColor,
        int begin, int end,
        MatrixPixelWriter next
    )
    {
        this.prev = prev;
        this.cur = cur;
        this.dest = dest;
        this.aliveColor = aliveColor;
        this.deadColor = deadColor;
        this.begin = begin;
        this.end = end;
        this.next = next;
    }

    public MatrixPixelWriter
    (
        Generation prev,
        Generation cur,
        PixelWriter dest,
        Color aliveColor,
        Color deadColor
    )
    {
        this
        (
            prev,
            cur,
            dest,
            aliveColor,
            deadColor,
            0,
            prev.getWidth(),
            null
        );
    }

    public MatrixPixelWriter
    (
        Generation prev,
        Generation cur,
        PixelWriter dest
    )
    {
        this
        (
            prev,
            cur,
            dest,
            ALIVE_COLOR,
            DEAD_COLOR
        );
    }

    @Override
    protected void compute()
    {
        MatrixPixelWriter right = null;
        int b = getBeginIx();
        int e = getEndIx();
        while(e - b > 1 && getSurplusQueuedTaskCount() <= SURPLUS_MAX)
        {
            int mid = (b + e) >>> 1;
            right = new MatrixPixelWriter
            (
                getPreviousGeneration(),
                getCurrentGeneration(),
                getDestination(),
                getAliveColor(),
                getDeadColor(),
                mid,
                e,
                right
            );
            right.fork();
            e = mid;
        }
        writePixels();
        while(right != null)
        {
            if (right.tryUnfork())
            {
                right.writePixels();
            }
            else
            {
                right.join();
            }
            right = right.getNext();
        }
    }

    private Generation getPreviousGeneration()
    {
        return this.prev;
    }

    private Generation getCurrentGeneration()
    {
        return this.cur;
    }

    private PixelWriter getDestination()
    {
        return this.dest;
    }

    private Color getAliveColor()
    {
        return this.aliveColor;
    }

    private Color getDeadColor()
    {
        return this.deadColor;
    }

    private MatrixPixelWriter getNext()
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

    private void writePixels()
    {
        for (int col = getBeginIx(); col < getEndIx(); col++)
        {
            for (int row = 0; row < getPreviousGeneration().getHeight(); row++)
            {
                boolean currentPopulationAlive =
                    getCurrentGeneration().isPopulationAlive(col, row);
                Color nColor = currentPopulationAlive
                    ? ALIVE_COLOR
                    : DEAD_COLOR;
                if
                (
                    currentPopulationAlive
                    != getPreviousGeneration().isPopulationAlive(col, row)
                )
                    getDestination().setColor(col, row, nColor);

            }
        }
    }

}
