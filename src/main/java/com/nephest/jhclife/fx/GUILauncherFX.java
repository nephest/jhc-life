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

import com.nephest.jhclife.*;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GUILauncherFX
extends Application
{

    public static final String MAIN_TITLE = "jhc-life";
    public static final String MAIN_TITLE_PREFIX = MAIN_TITLE + " — ";

    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;

    public static final String STYLESHEET_RESOURCE
        = "com/nephest/jhclife/resources/style/fx/default.css";

    private ClassicLifeModel model;
    private ExecutorService executor;

    @Override
    public void start(Stage stage)
    {
        this.model = new ClassicLifeModel(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.executor = Executors.newSingleThreadExecutor();

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene
        (
            new Group(),
            screen.getWidth() / 2,
            screen.getHeight() / 2
        );
        scene.getStylesheets().add(STYLESHEET_RESOURCE);
        stage.setScene(scene);
        stage.setTitle(MAIN_TITLE);

        MainMenuViewImplFX mainMenuView = new MainMenuViewImplFX();
        LifeViewImplFX lifeView = new LifeViewImplFX();
        mainMenuView.addExternalElementsCss(STYLESHEET_RESOURCE);
        lifeView.addExternalElementsCss(STYLESHEET_RESOURCE);
        Map<MainView.ViewType, ViewBase<Parent>> views
            = new EnumMap<>(MainView.ViewType.class);
        views.put(MainView.ViewType.MAIN_MENU, mainMenuView);
        views.put(MainView.ViewType.LIFE, lifeView);
        MainView<Parent> mainView = new MainViewImplFX(scene, views);

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
        stage.setMaximized(true);
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
