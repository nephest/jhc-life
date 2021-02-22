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

package com.nephest.jhclife.io;

import javafx.scene.input.*;

public class ScrollDirectionCombination
extends InputCombination<ScrollDirectionCombination.Direction>
{

    public enum Direction
    {
        UP, DOWN, NONE;
    }

    public ScrollDirectionCombination(Direction direction, KeyCombination.Modifier... modifiers)
    {
        super(direction, "Scroll", modifiers);
    }

    public boolean match(ScrollEvent evt)
    {
        if
        (
            getDirection(evt) != getTrigger()
            || evt.getEventType() != ScrollEvent.SCROLL
        )
        return false;

        boolean match = true;
        for (KeyCombination.Modifier mod : getModifiers())
        {
            switch(mod.getKey())
            {
                case CONTROL:
                {
                    if
                    (
                        (
                            mod.getValue() == KeyCombination.ModifierValue.UP
                            && evt.isControlDown()
                        )
                        ||
                        (
                            mod.getValue() == KeyCombination.ModifierValue.DOWN
                            && !evt.isControlDown()
                        )
                    ) match = false;
                    break;
                }
                case ALT:
                {
                    if
                    (
                        (
                            mod.getValue() == KeyCombination.ModifierValue.UP
                            && evt.isAltDown()
                        )
                        ||
                        (
                            mod.getValue() == KeyCombination.ModifierValue.DOWN
                            && !evt.isAltDown()
                        )
                    ) match = false;
                    break;
                }
                case SHIFT:
                {
                    if
                    (
                        (
                            mod.getValue() == KeyCombination.ModifierValue.UP
                            && evt.isShiftDown()
                        )
                        ||
                        (
                            mod.getValue() == KeyCombination.ModifierValue.DOWN
                            && !evt.isShiftDown()
                        )
                    ) match = false;
                    break;
                }
                case META:
                {
                    if
                    (
                        (
                            mod.getValue() == KeyCombination.ModifierValue.UP
                            && evt.isMetaDown()
                        )
                        ||
                        (
                            mod.getValue() == KeyCombination.ModifierValue.DOWN
                            && !evt.isMetaDown()
                        )
                    ) match = false;
                    break;
                }
                case SHORTCUT:
                {
                    if
                    (
                        (
                            mod.getValue() == KeyCombination.ModifierValue.UP
                            && evt.isShortcutDown()
                        )
                        ||
                        (
                            mod.getValue() == KeyCombination.ModifierValue.DOWN
                            && !evt.isShortcutDown()
                        )
                    ) match = false;
                    break;
                }
            }
        }
        return match;
    }

    private Direction getDirection(ScrollEvent evt)
    {
        Direction dir = Direction.NONE;
        if (evt.getDeltaY() < 0)
        {
            dir = Direction.DOWN;
        }
        else if (evt.getDeltaY() > 0)
        {
            dir = Direction.UP;
        }
        return dir;
    }

}

