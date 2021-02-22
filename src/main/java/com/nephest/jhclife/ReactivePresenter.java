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

package com.nephest.jhclife;

import java.util.Objects;
import java.util.concurrent.Executor;

public class ReactivePresenter
<
    V extends ReactiveViewBase<?, L, C>,
    M, L, C extends Enum<C>
>
extends PresenterBase<V, M>
{

    private final MainController mainController;
    private final Executor executor;
    private L listener;

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

    protected void setListener(L listener)
    {
        this.listener = listener;
        getView().setListener(listener);
    }

    public L getListener()
    {
        return this.listener;
    }

}
