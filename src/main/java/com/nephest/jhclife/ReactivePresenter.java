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

package com.nephest.jhclife;

import java.util.Objects;
import java.util.concurrent.Executor;

public class ReactivePresenter<V extends ViewBase, M>
extends PresenterBase<V, M>
{

    private final MainController mainController;
    private final Executor executor;

    public ReactivePresenter
    (
        V view,
        M model,
        MainController mainController,
        Executor executor
    )
    {
        super(view, model);
        Objects.requireNonNull(mainController);
        Objects.requireNonNull(executor);
        this.mainController = mainController;
        this.executor = executor;
    }

    protected MainController getMainController()
    {
        return this.mainController;
    }

    protected Executor getExecutor()
    {
        return this.executor;
    }

}