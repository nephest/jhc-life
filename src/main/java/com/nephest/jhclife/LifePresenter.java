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

import com.nephest.jhclife.io.*;
import com.nephest.jhclife.util.ObjectTranslator;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.*;

import javafx.scene.input.*;

public class LifePresenter
extends ReactivePresenter<LifeView<?>, ClassicLifeModel, LifeViewListener>
{

    private static final Logger LOG = Logger.getLogger(LifePresenter.class.getName());

    public static final double ZOOM_FACTOR_UP = 2;
    public static final double ZOOM_FACTOR_DOWN = 0.5;
    public static final double ZOOM_FACTOR_INIT = 0.0;
    public static final String ZOOM_FORMAT="%06.2f";

    public static final int SPEED_STEP = 1;
    public static final int SPEED_INIT = 10;
    public static final String SPEED_FORMAT="%03d";

    public static final KeyCode PLAY_TOGGLE = KeyCode.SPACE;
    public static final KeyCode PLAY_TOGGLE_ALT = KeyCode.P;

    public static final KeyCode NEW_GAME = KeyCode.ESCAPE;
    public static final KeyCode GENERATION_SAVE = KeyCode.S;
    public static final KeyCode GENERATION_LOAD = KeyCode.O;

    public static final String HELP_MSG =
        "Info:\n"
        + "This is a basic Conway's Game of Life implementation.\n"
        + "\n"
        + "Rules:\n"
        + "Any live cell with fewer than two live neighbors dies.\n"
        + "Any live cell with two or three live neighbors lives on.\n"
        + "Any live cell with more than three live neighbors dies.\n"
        + "Any dead cell with exactly three live neighbors becomes a live cell.\n"
        + "\n"
        + "Binds:\n"
        + "zoom+\t\tctrl+MouseLeft\t| ctrl+ScrollUp\n"
        + "zoom-\t\tctrl+MouseRight\t| ctrl+ScrollDown\n"
        + "speed+\t\talt+MouseLeft\t\t| alt+ScrollUp\n"
        + "speed-\t\talt+MouseRight\t| alt+ScrollDown\n"
        + "play/pause\tspace\t| p\n"
        + "population\tmouseClick\n"
        + "\n"
        + "Misc:\n"
        + "nephest.com/projects/jhc-life\n"
        + "GPL Version 3\n"
        + "Copyright (C) 2018 Oleksandr Masniuk\n";


    private FileIO fileIO = new StandardFileIO();
    private ObjectTranslator<Generation> generationTranslator;

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
        initTranslators();
        listen();
        initInfo();
    }

    private void initTranslators()
    {
        this.generationTranslator = new ObjectTranslator<Generation>()
        {

            @Override
            public byte[] toByteArray(Generation generation)
            {
                return Generation.toByteArray(generation);
            }

            @Override
            public Generation fromByteArray(byte[] bytes)
            {
                return Generation.fromByteArray(bytes);
            }

        };
    }

    private void initInfo()
    {
        changeSpeed(SPEED_INIT);
        changeZoom(ZOOM_FACTOR_INIT);
    }

    private void listen()
    {
        LifeViewListener listener = new LifeViewListener()
        {
            @Override
            public void onMouseEvent(MouseEvent evt, LifeView.Zone zone)
            {
                if (mustConsumeEvent(evt, zone)) evt.consume();
                getExecutor().execute(()->mouseEvent(evt, zone));
            }

            @Override
            public void onScrollEvent(ScrollEvent evt, LifeView.Zone zone)
            {
                if (mustConsumeEvent(evt, zone)) evt.consume();
                getExecutor().execute(()->scrollEvent(evt, zone));
            }

            @Override
            public void onKeyEvent(KeyEvent evt, LifeView.Zone zone)
            {
                if (mustConsumeEvent(evt, zone)) evt.consume();
                getExecutor().execute(()->keyEvent(evt, zone));
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
            public void onGenerationSave()
            {
                getExecutor().execute(()->generationSave());
            }

            @Override
            public void onGenerationLoad()
            {
                getExecutor().execute(()->generationLoad());
            }

            @Override
            public void onHelp()
            {
                getExecutor().execute(()->help());
            }

            @Override
            public void readyForNextFrame()
            {
                nextFrame(); //directly in render thread to avoid skipped frames
            }
        };
        setListener(listener);
    }

    private void mouseEvent(MouseEvent evt, LifeView.Zone zone)
    {
        Objects.requireNonNull(evt);
        Objects.requireNonNull(zone);
        if (evt.getEventType() == MouseEvent.MOUSE_CLICKED)
        {
            mouseClick(evt, zone);
        }
    }

    private void mouseClick(MouseEvent evt, LifeView.Zone zone)
    {
        if (evt.isControlDown())
        {
            changeZoom(evt, zone);
        }
        else if (evt.isAltDown())
        {
            changeSpeed(evt, zone);
        }
        else
        {
            togglePopulation(evt, zone);
        }
    }

    private void changeZoom(MouseEvent evt, LifeView.Zone zone)
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
        if (zone == LifeView.Zone.GLOBAL)
        {
            changeZoom(factor);
        }
        else if (zone == LifeView.Zone.GENERATION_CONTAINER)
        {
            changeZoom(factor, (int) evt.getX(), (int) evt.getY());
        }
        //generation zoom event is not consumed is and passed to its container
    }

    private void changeSpeed(MouseEvent evt, LifeView.Zone zone)
    {
        if (zone == LifeView.Zone.GENERATION) return;
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

    private void togglePopulation(MouseEvent evt, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GENERATION || !evt.isStillSincePress()) return;
        int x = (int) evt.getX();
        int y = (int) evt.getY();
        boolean pop = getLastGeneration().isPopulationAlive(x, y);
        getModel().setPopulation(x, y, !pop);
    }

    private boolean mustConsumeEvent(MouseEvent evt, LifeView.Zone zone)
    {
        return zone != LifeView.Zone.GENERATION
            && (evt.isControlDown() || evt.isAltDown());
    }

    private boolean mustConsumeEvent(ScrollEvent evt, LifeView.Zone zone)
    {
        return zone != LifeView.Zone.GENERATION
            && (evt.isControlDown() || evt.isAltDown());
    }

    private boolean mustConsumeEvent(KeyEvent evt, LifeView.Zone zone)
    {
        return zone == LifeView.Zone.GLOBAL
            &&
            (
                evt.getCode() == PLAY_TOGGLE
                || evt.getCode() == PLAY_TOGGLE_ALT
                || evt.getCode() == NEW_GAME
                || evt.getCode() == GENERATION_SAVE
                || evt.getCode() == GENERATION_LOAD
            );
    }

    private void scrollEvent(ScrollEvent evt, LifeView.Zone zone)
    {
        Objects.requireNonNull(evt);
        Objects.requireNonNull(zone);

        if (evt.getEventType() == ScrollEvent.SCROLL)
        {
            scrollScroll(evt, zone);
        }
    }

    private void scrollScroll(ScrollEvent evt, LifeView.Zone zone)
    {
        if (evt.isControlDown())
        {
            changeZoom(evt, zone);
        }
        else if (evt.isAltDown())
        {
            changeSpeed(evt, zone);
        }
    }

    private void changeZoom(ScrollEvent evt, LifeView.Zone zone)
    {
        double factor = evt.getDeltaY() < 0 ? ZOOM_FACTOR_DOWN : ZOOM_FACTOR_UP;
        if (zone == LifeView.Zone.GLOBAL)
        {
            changeZoom(factor);
        }
        else if (zone == LifeView.Zone.GENERATION_CONTAINER)
        {
            changeZoom(factor, (int) evt.getX(), (int) evt.getY());
        }
        //generation zoom event is not consumed and is passed to its container
    }

    private void changeSpeed(ScrollEvent evt, LifeView.Zone zone)
    {
        if (zone == LifeView.Zone.GENERATION) return;
        int delta = evt.getDeltaY() < 0 ? -SPEED_STEP : SPEED_STEP;
        changeSpeed(getSpeed() + delta);
    }

    private void keyEvent(KeyEvent evt, LifeView.Zone zone)
    {
        Objects.requireNonNull(evt);
        Objects.requireNonNull(zone);

        if (evt.getEventType() == KeyEvent.KEY_PRESSED)
        {
            keyPressed(evt, zone);
        }
    }

    private void keyPressed(KeyEvent evt, LifeView.Zone zone)
    {
        if
        (
            (evt.getCode() == PLAY_TOGGLE || evt.getCode() == PLAY_TOGGLE_ALT)
            && zone == LifeView.Zone.GLOBAL
        )
        {
            toggleState();
        }
        else if
        (
            evt.getCode() == NEW_GAME
            && zone == LifeView.Zone.GLOBAL
        )
        {
            getListener().onNewGame();
        }
        else if
        (
            evt.getCode() == GENERATION_SAVE
            && evt.isControlDown()
            && zone == LifeView.Zone.GLOBAL
        )
        {
            getListener().onGenerationSave();
        }
        else if
        (
            evt.getCode() == GENERATION_LOAD
            && evt.isControlDown()
            && zone == LifeView.Zone.GLOBAL
        )
        {
            getListener().onGenerationLoad();
        }
    }

    private void toggleState()
    {
        if (getModel().isRunning())
        {
            getModel().stop();
        }
        else
        {
            getModel().start();
        }
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

    private void generationSave()
    {
        if (getLastGeneration() == null)
        {
            getView().fireErrorAlert("No generation to save", "");
            return;
        }
        Generation toSave = getLastGeneration();
        getView().selectFile
        (
            ViewBase.FileSelectionMode.SAVE,
            "Choose a save filename",
            "life-generation-"
                + toSave.getId() + "-"
                + toSave.getGenerationNumber(),
            (files)->
            {
                if (files.size() > 0)
                    getExecutor().execute( ()->generationSaveSelected(files.get(0), toSave) );
            }
        );
    }

    private void generationSaveSelected(File file, Generation generation)
    {
        if (file.exists())
        {
            getView().fireConfirmationAlert
            (
                "File already exists",
                "Do you want to overwrite the existing file?",
                ()->{ getExecutor().execute(()->doSaveGeneration(file, generation)); },
                null
            );
        }
        else
        {
            doSaveGeneration(file, generation);
        }
    }

    private void doSaveGeneration(File file, Generation generation)
    {
        try
        {
            getFileIO().write
            (
                file.toPath(),
                getGenerationTranslator().toByteArray(generation)
            );
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            getView().fireErrorAlert("Generation saving failed", ex.getMessage());
        }
    }

    private void generationLoad()
    {
        getView().selectFile
        (
            ViewBase.FileSelectionMode.SELECT_SINGLE,
            "Choose a generation save to load",
            "",
            (files)->
            {
                if (files.size() > 0)
                    getExecutor().execute( ()->generationLoadSelected(files.get(0)) );
            }
        );
    }

    private void generationLoadSelected(File file)
    {
        if (!file.exists())
        {
            getView().fireErrorAlert("Generation loading failed", "No such file");
            return;
        }
        try
        {
            byte[] bytes = getFileIO().readAllBytes(file.toPath());
            Generation gen = getGenerationTranslator().fromByteArray(bytes);
            getModel().setGeneration(gen);
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            getView().fireErrorAlert("Generation loading failed", ex.getMessage());
        }
        catch (IllegalArgumentException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            getView().fireErrorAlert("Generation loading failed", "Invalid save file");
        }
    }

    private void help()
    {
        getView().fireInfoAlert("Help", HELP_MSG);
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

    public void setFileIO(FileIO io)
    {
        this.fileIO = io;
    }

    public FileIO getFileIO()
    {
        return this.fileIO;
    }

    public void setGenerationTranslator(ObjectTranslator<Generation> translator)
    {
        this.generationTranslator = translator;
    }

    public ObjectTranslator<Generation> getGenerationTranslator()
    {
        return this.generationTranslator;
    }

    private Generation getLastGeneration()
    {
        return this.lastGeneration;
    }

    private void changeZoom(double factor, int x, int y)
    {
        getView().setGenerationZoom(factor, x, y);
        getView().updateZoomInfo(ZOOM_FORMAT);
    }

    private void changeZoom(double factor)
    {
        getView().setGenerationZoom(factor);
        getView().updateZoomInfo(ZOOM_FORMAT);
    }

    private void changeSpeed(int speed)
    {
        speed = speed < 1 ? 1 : speed;
        long nanos = 1_000_000_000;
        long period = nanos / speed;
        getModel().setGenerationLifeTime(period, TimeUnit.NANOSECONDS);
        getView().setSpeedInfo(String.format(SPEED_FORMAT, speed));
        this.speed = speed;
    }

    public int getSpeed()
    {
        return this.speed;
    }
}
