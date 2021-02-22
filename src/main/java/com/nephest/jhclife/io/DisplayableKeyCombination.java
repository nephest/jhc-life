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

import javafx.scene.input.KeyCombination;

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
