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

import com.nephest.jhclife.MainView;
import com.nephest.jhclife.ViewBase;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainViewImplFX
extends ViewBaseImplFX
implements MainView<Parent>
{

    private static final Logger LOG
        = Logger.getLogger(MainViewImplFX.class.getName());
    public static final MainView.ViewType DEFAULT_VIEW
        = MainView.ViewType.MAIN_MENU;

    private final Scene mainScene;
    private final Map<MainView.ViewType, ViewBase<Parent>> views;

    private volatile MainView.ViewType currentViewType;

    public MainViewImplFX
    (
        Scene mainScene,
        Map<MainView.ViewType, ViewBase<Parent>> views
    )
    {
        super();
        Objects.requireNonNull(mainScene);
        Objects.requireNonNull(views);
        this.mainScene = mainScene;
        this.views = views;
        init();
    }

    @Override
    public Parent getRoot()
    {
        return null;
    }

    @Override
    public void setViewType(MainView.ViewType type)
    {
        Platform.runLater( ()->doSetViewType(type) );
    }

    private void doSetViewType(MainView.ViewType type)
    {
        getMainScene().setRoot( getViews().get(type).getRoot() );
        this.currentViewType = type;
        LOG.log(Level.FINE, "View type set: {0}", new Object[]{type});
    }

    @Override
    public MainView.ViewType getCurrentViewType()
    {
        return this.currentViewType;
    }

    private Scene getMainScene()
    {
        return this.mainScene;
    }

    private Map<MainView.ViewType, ViewBase<Parent>> getViews()
    {
        return this.views;
    }

    private void init()
    {
        setViewType(DEFAULT_VIEW);
    }

}
