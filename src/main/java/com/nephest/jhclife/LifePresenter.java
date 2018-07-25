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
import java.util.concurrent.*;

import javafx.scene.input.*;

public class LifePresenter
extends ReactivePresenter<LifeView<?>, ClassicLifeModel>
{

    public static final double ZOOM_FACTOR_UP = 1.5;
    public static final double ZOOM_FACTOR_DOWN = 0.5;
    public static final double ZOOM_FACTOR_INIT = 0.0;

    public static final int SPEED_STEP = 1;
    public static final int SPEED_INIT = 4;

    private Generation lastGeneration;
    private int speed = SPEED_INIT;

    public LifePresenter
    (
        LifeView<?> view,
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
        listen();
    }

    private void listen()
    {
        LifeViewListener listener = new LifeViewListener()
        {
            @Override
            public void onMouseEvent(MouseEvent evt)
            {
                getExecutor().execute(()->mouseEvent(evt));
            }

            @Override
            public void onScrollEvent(ScrollEvent evt)
            {
                if (mustConsumeEvent(evt)) evt.consume();
                getExecutor().execute(()->scrollEvent(evt));
            }

            @Override
            public void onZoomUp()
            {
                getExecutor().execute(()->zoomUp());
            }

            @Override
            public void onZoomDown()
            {
                getExecutor().execute(()->zoomDown());
            }

            @Override
            public void onZoomDefault()
            {
                getExecutor().execute(()->zoomDefault());
            }

            @Override
            public void onSpeedUp()
            {
                getExecutor().execute(()->speedUp());
            }

            @Override
            public void onSpeedDown()
            {
                getExecutor().execute(()->speedDown());
            }

            @Override
            public void onSpeedDefault()
            {
                getExecutor().execute(()->speedDefault());
            }

            @Override
            public void onPause()
            {
                getExecutor().execute(()->pause());
            }

            @Override
            public void onPlay()
            {
                getExecutor().execute(()->play());
            }

            @Override
            public void onNewGame()
            {
                getExecutor().execute(()->newGame());
            }

            @Override
            public void readyForNextFrame()
            {
                nextFrame(); //directly in render thread to avoid skipped frames
            }
        };
        getView().setListener(listener);
    }

    private void mouseEvent(MouseEvent evt)
    {
        Objects.requireNonNull(evt);

        if (evt.getEventType() == MouseEvent.MOUSE_CLICKED)
        {
            mouseClick(evt);
        }
    }

    private void mouseClick(MouseEvent evt)
    {
        if (evt.isControlDown())
        {
            changeZoom(evt);
        }
        else if (evt.isShiftDown())
        {
            changeSpeed(evt);
        }
        else
        {
            togglePopulation(evt);
        }
    }

    private void changeZoom(MouseEvent evt)
    {
        double factor = ZOOM_FACTOR_INIT;
        switch(evt.getButton())
        {
            case PRIMARY:
                factor = ZOOM_FACTOR_UP;
                break;
            case SECONDARY:
                factor = ZOOM_FACTOR_DOWN;
                break;
            case MIDDLE:
                factor = ZOOM_FACTOR_INIT;
                break;
        }
        changeZoom(factor, (int) evt.getX(), (int) evt.getY());
    }

    private void changeSpeed(MouseEvent evt)
    {
        int speed = getSpeed();
        switch(evt.getButton())
        {
            case PRIMARY:
                speed += SPEED_STEP;
                break;
            case SECONDARY:
                speed -= SPEED_STEP;
                break;
            case MIDDLE:
                speed = SPEED_INIT;
                break;
        }
        changeSpeed(speed);
    }

    private void togglePopulation(MouseEvent evt)
    {
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        boolean pop = getLastGeneration().isPopulationAlive(x, y);
        getModel().setPopulation(x, y, !pop);
    }

    private boolean mustConsumeEvent(ScrollEvent evt)
    {
        return evt.isControlDown() || evt.isShiftDown();
    }

    private void scrollEvent(ScrollEvent evt)
    {
        Objects.requireNonNull(evt);

        if (evt.getEventType() == ScrollEvent.SCROLL)
        {
            scrollScroll(evt);
        }
    }

    private void scrollScroll(ScrollEvent evt)
    {
        if (evt.isControlDown())
        {
            changeZoom(evt);
        }
        else if (evt.isShiftDown())
        {
            changeSpeed(evt);
        }
    }

    private void changeZoom(ScrollEvent evt)
    {
        double factor = evt.getDeltaY() < 0 ? ZOOM_FACTOR_DOWN : ZOOM_FACTOR_UP;
        changeZoom(factor, (int) evt.getX(), (int) evt.getY());
    }

    private void changeSpeed(ScrollEvent evt)
    {
        int delta = evt.getDeltaY() < 0 ? -SPEED_STEP : SPEED_STEP;
        changeSpeed(getSpeed() + delta);
    }

    private void zoomUp()
    {
        changeZoom(ZOOM_FACTOR_UP);
    }

    private void zoomDown()
    {
        changeZoom(ZOOM_FACTOR_DOWN);
    }

    private void zoomDefault()
    {
        changeZoom(ZOOM_FACTOR_INIT);
    }

    private void speedUp()
    {
        changeSpeed(getSpeed() + SPEED_STEP);
    }

    private void speedDown()
    {
        changeSpeed(getSpeed() - SPEED_STEP);
    }

    private void speedDefault()
    {
        changeSpeed(SPEED_INIT);
    }

    private void pause()
    {
        getModel().stop();
    }

    private void play()
    {
        getModel().start();
    }

    private void newGame()
    {
        getModel().stop();
        getMainController().setViewType(MainView.ViewType.MAIN_MENU);
    }

    private void nextFrame()
    {
        Generation cur = getModel().getLastGeneration();
        Generation last = getLastGeneration();
        if
        (
            last == null
            || cur.getId() != last.getId()
            || cur.getGenerationNumber() != last.getGenerationNumber()
        )
        {
            getView().render(cur);
            this.lastGeneration = cur;
        }
    }

    private Generation getLastGeneration()
    {
        return this.lastGeneration;
    }

    private void changeZoom(double factor, int x, int y)
    {
        getView().setGenerationZoom(factor, x, y);
        getView().updateZoomInfo();
    }

    private void changeZoom(double factor)
    {
        getView().setGenerationZoom(factor);
        getView().updateZoomInfo();
    }

    private void changeSpeed(int speed)
    {
        speed = speed < 1 ? 1 : speed;
        long nanos = 1_000_000_000;
        long period = nanos / speed;
        getModel().setGenerationLifeTime(period, TimeUnit.NANOSECONDS);
        getView().setSpeedInfo(speed);
        this.speed = speed;
    }

    public int getSpeed()
    {
        return this.speed;
    }
}
