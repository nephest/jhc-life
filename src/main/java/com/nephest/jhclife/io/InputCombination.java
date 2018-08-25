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

public class InputCombination<T>
{

    private final T trigger;
    private final KeyCombination.Modifier[] modifiers;

    private final String prefix;
    private String displayText;

    public InputCombination
    (
        T trigger,
        String prefix,
        KeyCombination.Modifier... modifiers
    )
    {
        this.trigger = trigger;
        this.modifiers = modifiers;
        this.prefix = prefix;
    }

    protected T getTrigger()
    {
        return this.trigger;
    }

    protected KeyCombination.Modifier[] getModifiers()
    {
        return this.modifiers;
    }

    public String getPrefix()
    {
        return this.prefix;
    }

    public static final String getShortcutKeyString()
    {
        return System.getProperty("os.name").toLowerCase().contains("mac ")
            ? "Meta"
            : "Ctrl";
    }

    public void appendKeyModifiers(StringBuilder sb)
    {
        for (KeyCombination.Modifier mod : getModifiers())
        {
            if (mod.getKey() == KeyCode.SHORTCUT)
            {
                sb.append(InputCombination.getShortcutKeyString());
            }
            else
            {
                sb.append(mod.toString());
            }
            sb.append("+");
        }
    }

    public String getDisplayText()
    {
        if (this.displayText == null)
        {
            StringBuilder sb = new StringBuilder();
            appendKeyModifiers(sb);
            sb.append(getPrefix())
                .append(getTrigger().toString().substring(0, 1).toUpperCase())
                .append(getTrigger().toString().substring(1).toLowerCase());
            this.displayText = sb.toString();
        }
        return this.displayText;
    }

}
