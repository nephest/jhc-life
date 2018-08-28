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

import java.util.*;

public class ControlBindings<T extends Enum<T>, B>
{

    private final Class<T> type;
    private final Map<T, B> binds;

    public ControlBindings(Class<T> type)
    {
        this.type = type;
        this.binds = new EnumMap(type);
    }

    public static <C extends Enum<C>> String calculateBindingsString
    (
        String splitter,
        C ctrl,
        ControlBindings<C, ? extends Displayable>... binds
    )
    {
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (int i = 0; i < binds.length; i++)
        {
            Displayable displayable = binds[i].getBinding(ctrl);
            String str = displayable == null ? null : displayable.getDisplayText();
            if (str != null && !str.isEmpty())
            {
                if (found)
                {
                    sb.append(splitter);
                }
                sb.append(str);
                found = true;
            }
        }
        return sb.toString();
    }

    public static <C extends Enum<C>> String calculateControlName
    (
        String name,
        String nameSplitter,
        String prefix,
        String splitter,
        String suffix,
        C ctrl,
        ControlBindings<C, ? extends Displayable>... binds
    )
    {
        String bindsStr = calculateBindingsString(splitter, ctrl, binds);
        if (bindsStr.isEmpty()) return name;

        StringBuilder sb = new StringBuilder(name);
        sb.append(nameSplitter);
        sb.append(prefix);
        sb.append(bindsStr);
        sb.append(suffix);
        return sb.toString();
    }

    public static <C extends Enum<C>> String calculateControlName
    (
        String name,
        String nameSplitter,
        String splitter,
        C ctrl,
        ControlBindings<C, ? extends Displayable>... binds
    )
    {
        return calculateControlName(name, nameSplitter, "", splitter, "", ctrl, binds);
    }

    public Class<T> getType()
    {
        return this.type;
    }

    public void setBinding(T ctrl, B bind)
    {
        if (bind == null)
        {
            this.binds.remove(ctrl);
        }
        else
        {
            this.binds.put(ctrl, bind);
        }
    }

    public B getBinding(T ctrl)
    {
        return this.binds.get(ctrl);
    }

}

