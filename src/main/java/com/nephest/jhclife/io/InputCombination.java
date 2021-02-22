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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

public class InputCombination<T>
implements Displayable
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

    public static String getShortcutKeyString()
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

    @Override
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
