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

import com.nephest.jhclife.*;

import java.util.*;
import java.util.logging.*;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.*;

public class MainViewImplFX
extends ViewBaseImplFX<Scene>
implements MainView<Scene>
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
    public Scene getRoot()
    {
        return getMainScene();
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
