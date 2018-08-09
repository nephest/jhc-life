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

import java.util.logging.*;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javafx.scene.control.*;

public final class FX
{

    public static final Logger LOG = Logger.getLogger(FX.class.getName());

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
