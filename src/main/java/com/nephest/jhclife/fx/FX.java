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

package com.nephest.jhclife.fx;

import java.util.logging.*;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javafx.scene.control.*;

public final class FX
{

    private static final Logger LOG = Logger.getLogger(FX.class.getName());

    public static final String CONTROL_NAME_SPLITTER = " ";
    public static final String CONTROL_PREFIX = "[";
    public static final String CONTROL_SUFFIX = "]";
    public static final String CONTROL_SPLITTER = " | ";

    private FX() {}

    public static final <T> void filteredSpinner
    (
        Spinner<T> spinner,
        T def,
        T empty
    )
    {
        Objects.requireNonNull(spinner);
        Objects.requireNonNull(def);
        Objects.requireNonNull(empty);

        UnaryOperator<TextFormatter.Change> filter = (c)->
        {
            if (!c.isContentChange()) return c;

            TextFormatter.Change result = c;
            if (c.getControlNewText().isEmpty())
            {
                c.setRange(0, c.getControlText().length());
                c.setText(spinner.getValueFactory().getConverter().toString(empty));
            }
            else
            {
                try
                {
                    spinner.getValueFactory().getConverter()
                        .fromString(c.getControlNewText());
                }
                catch (RuntimeException ex)
                {
                    LOG.log(Level.FINEST, ex.getMessage(), ex);
                    result = null;
                }
            }
            return result;
        };

        TextFormatter formatter = new TextFormatter
        (
            spinner.getValueFactory().getConverter(),
            def,
            filter
        );
        spinner.getEditor().setTextFormatter(formatter);
    }

    public static final void focusCommitedSpinner(Spinner spinner)
    {
        Objects.requireNonNull(spinner);

        spinner.focusedProperty()
            .addListener((l, ov, nv)->{if (!nv) spinner.increment(0);});
    }

    public static final <T> void standardSpinner
    (
        Spinner<T> spinner,
        T def,
        T empty
    )
    {
        FX.filteredSpinner(spinner, def, empty);
        FX.focusCommitedSpinner(spinner);
    }

}
