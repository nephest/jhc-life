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

import java.util.EnumMap;
import java.util.Map;

public class ControlBindings<T extends Enum<T>, B>
{

    private final Class<T> type;
    private final Map<T, B> binds;

    public ControlBindings(Class<T> type)
    {
        this.type = type;
        this.binds = new EnumMap<>(type);
    }

    @SafeVarargs
    public static <C extends Enum<C>> String calculateBindingsString
    (
        String splitter,
        C ctrl,
        ControlBindings<C, ? extends Displayable>... binds
    )
    {
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for (ControlBindings<C, ? extends Displayable> bind : binds)
        {
            Displayable displayable = bind.getBinding(ctrl);
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

    @SafeVarargs
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

        return name + nameSplitter + prefix + bindsStr + suffix;
    }

    @SafeVarargs
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

