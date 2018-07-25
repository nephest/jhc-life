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
import java.util.concurrent.*;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class GUILauncherFX
extends Application
{

    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;

    private ClassicLifeModel model;
    private ExecutorService executor;

    @Override
    public void start(Stage stage)
    {
        this.model = new ClassicLifeModel(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.executor = Executors.newSingleThreadExecutor();

        MainMenuView mainMenuView = new MainMenuViewImplFX();
        LifeView lifeView = new LifeViewImplFX();
        Map<MainView.ViewType, ViewBase<Parent>> views
            = new EnumMap(MainView.ViewType.class);
        views.put(MainView.ViewType.MAIN_MENU, mainMenuView);
        views.put(MainView.ViewType.LIFE, lifeView);
        MainView mainView = new MainViewImplFX(stage, views);

        MainController mainController = new MainController(mainView);
        MainMenuPresenter MainMenuPresenter = new MainMenuPresenter
        (
            mainMenuView,
            getModel(),
            mainController,
            getExecutor()
        );
        LifePresenter lifePresenter = new LifePresenter
        (
            lifeView,
            getModel(),
            mainController,
            getExecutor()
        );

        stage.show();
    }

    @Override
    public void stop()
    {
        getModel().close();
        getExecutor().shutdown();
    }

    private ClassicLifeModel getModel()
    {
        return this.model;
    }

    private ExecutorService getExecutor()
    {
        return this.executor;
    }

}
