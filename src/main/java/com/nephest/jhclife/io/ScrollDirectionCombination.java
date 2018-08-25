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
