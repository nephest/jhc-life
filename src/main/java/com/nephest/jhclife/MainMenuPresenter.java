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

import com.nephest.jhclife.io.*;

import java.util.*;
import java.util.concurrent.Executor;

import javafx.scene.input.*;

public class MainMenuPresenter
extends ReactivePresenter
<
    MainMenuView<?>,
    ClassicLifeModel,
    MainMenuViewListener,
    MainMenuPresenter.ControlType
>
{

    public static enum ControlType
    {
        NEW_GAME, CANCEL;
    }

    private final Map<ControlType, EventConsumer<MainMenuView.Zone>> controlActions
        = new EnumMap(ControlType.class);

    public static final KeyCombination DEFAULT_CANCEL_COMBINATION
        = LifePresenter.DEFAULT_NEW_GAME_COMBINATION;

    private final ControlBindings<ControlType, KeyCombination> keyControl
        = new ControlBindings(ControlType.class);

    public MainMenuPresenter
    (
        MainMenuView<?> view,
        ClassicLifeModel model,
        MainController mainController,
        Executor executor
    )
    {
        super(view, model, mainController, executor);
        init();
    }

    private void init()
    {
        initActions();
        initKeyControl();
        initControlBindingsInfo();
        listen();
    }

    private void initActions()
    {
        getControlActions().put
        (
            ControlType.NEW_GAME,
            (x, y, zone)->{ newGame(x, y, zone); }
        );

        getControlActions().put
        (
            ControlType.CANCEL,
            (x, y, zone)->{ cancel(x, y, zone); }
        );
    }

    private void initKeyControl()
    {
        getKeyControl().setBinding
        (
            ControlType.CANCEL,
            DEFAULT_CANCEL_COMBINATION
        );
    }

    private void initControlBindingsInfo()
    {
        getView().setControlBindingsInfo
            (DisplayableKeyCombination.toDisplayable(getKeyControl()));
    }

    private void listen()
    {
        MainMenuViewListener listener = new MainMenuViewListener()
        {

            @Override
            public void onKeyEvent(KeyEvent evt, MainMenuView.Zone zone)
            {
                if (mustConsumeEvent(evt, zone)) evt.consume();
                getExecutor().execute(()->keyEvent(evt, zone));
            }

            @Override
            public void onNewGame()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.NEW_GAME)
                        .consume(Double.NaN, Double.NaN, MainMenuView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onCancel()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.CANCEL)
                        .consume(Double.NaN, Double.NaN, MainMenuView.Zone.GLOBAL);
                    }
                );
            }
        };
        setListener(listener);
    }

    private boolean mustConsumeEvent(KeyEvent evt, MainMenuView.Zone zone)
    {
        if (zone != MainMenuView.Zone.GLOBAL) return false;

        boolean match = false;
        for (ControlType type : ControlType.values())
        {
            KeyCombination bind = getKeyControl().getBinding(type);
            if (bind != null && bind.match(evt))
            {
                match = true;
                break;
            }
        }
        return match;
    }

    private void keyEvent(KeyEvent evt, MainMenuView.Zone zone)
    {
        Objects.requireNonNull(evt);
        Objects.requireNonNull(zone);

        if (evt.getEventType() == KeyEvent.KEY_PRESSED)
        {
            keyPressed(evt, zone);
        }
    }

    private void keyPressed(KeyEvent evt, MainMenuView.Zone zone)
    {
        for (ControlType type : ControlType.values())
        {
            KeyCombination bind = getKeyControl().getBinding(type);
            if (bind != null && bind.match(evt))
            {
                getControlActions().get(type)
                    .consume(Double.NaN, Double.NaN, zone);
                break;
            }
        }
    }

    private void newGame(double x, double y, MainMenuView.Zone zone)
    {
        if (zone != MainMenuView.Zone.GLOBAL) return;

        getView().lock();
        if ( !checkNewGameParameters() )
        {
            getView().unlock();
            return;
        }
        getModel().stop();
        //view popProb is a percentage
        double popProb = getView().getPopulationProbability() / 100.0;
        getModel().createNewPopulation(getView().getWidth(), getView().getHeight());
        getModel().populate(getView().getSeed(), popProb);
        getMainController().setViewType(MainView.ViewType.LIFE);
        getView().unlock();
    }

    private boolean checkNewGameParameters()
    {
        return checkDimensions() && checkPopulationProbability();
    }

    private boolean checkDimensions()
    {
        boolean result = true;
        if (getView().getWidth() < 1 || getView().getHeight() < 1)
        {
            getView().fireErrorAlert
            (
                "Invalid dimensions",
                "Width and Height must be more than 0"
            );
            result = false;
        }
        return result;
    }

    private boolean checkPopulationProbability()
    {
        boolean result = true;
        double probability = getView().getPopulationProbability();
        if (probability < 0 || probability > 100)
        {
            getView().fireErrorAlert
            (
                "Invalid density",
                "Population density must be in 0-100 range"
            );
            result = false;
        }
        return result;
    }

    private void cancel(double x, double y, MainMenuView.Zone zone)
    {
        if (zone != MainMenuView.Zone.GLOBAL) return;
        getMainController().setViewType(MainView.ViewType.LIFE);
    }

    private Map<ControlType, EventConsumer<MainMenuView.Zone>> getControlActions()
    {
        return this.controlActions;
    }

    public ControlBindings<ControlType, KeyCombination> getKeyControl()
    {
        return this.keyControl;
    }

}
