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

public class DisplayableKeyCombination
implements Displayable
{

    private final KeyCombination combination;

    public DisplayableKeyCombination(KeyCombination combination)
    {
        this.combination = combination;
    }

    public static final <T extends Enum<T>, B extends KeyCombination>
    ControlBindings<T, DisplayableKeyCombination>
    toDisplayable(ControlBindings<T, B> binds)
    {
        ControlBindings<T, DisplayableKeyCombination> result
            = new ControlBindings(binds.getType());
        for (T enumConst : binds.getType().getEnumConstants())
        {
            result.setBinding
            (
                enumConst,
                new DisplayableKeyCombination(binds.getBinding(enumConst))
            );
        }
        return result;
    }

    @Override
    public String getDisplayText()
    {
        return combination == null ? null : combination().getDisplayText();
    }

    public KeyCombination combination()
    {
        return this.combination;
    }

}
