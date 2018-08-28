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
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import javafx.scene.input.*;

public class LifePresenter
extends ReactivePresenter
<
    LifeView<?>,
    ClassicLifeModel,
    LifeViewListener,
    LifePresenter.ControlType
>
{

    private static final Logger LOG = Logger.getLogger(LifePresenter.class.getName());

    public static enum ControlType
    {
        NEW_GAME,
        GENERATION_LOAD,
        GENERATION_SAVE,
        HELP,
        STATE_TOGGLE,
        ZOOM_UP,
        ZOOM_DOWN,
        ZOOM_DEFAULT,
        SPEED_UP,
        SPEED_DOWN,
        SPEED_DEFAULT,
        POPULAITON_TOGGLE;
    }

    private final Map<ControlType, EventConsumer<LifeView.Zone>> controlActions
        = new EnumMap(ControlType.class);

    public static final double ZOOM_FACTOR_UP = 2;
    public static final double ZOOM_FACTOR_DOWN = 0.5;
    public static final double ZOOM_FACTOR_INIT = 0.0;
    public static final String ZOOM_FORMAT="%06.2f";

    public static final int SPEED_STEP = 1;
    public static final int SPEED_INIT = 10;
    public static final String SPEED_FORMAT="%03d";

    public static final MouseKeyCombination DEFAULT_MOUSE_SPEED_UP_COMBINATION
        = new MouseKeyCombination(MouseButton.PRIMARY, KeyCodeCombination.ALT_DOWN);
    public static final MouseKeyCombination DEFAULT_MOUSE_SPEED_DOWN_COMBINATION
        = new MouseKeyCombination(MouseButton.SECONDARY, KeyCodeCombination.ALT_DOWN);
    public static final MouseKeyCombination DEFAULT_MOUSE_SPEED_DEFAULT_COMBINATION
        = new MouseKeyCombination(MouseButton.MIDDLE, KeyCodeCombination.ALT_DOWN);

    public static final MouseKeyCombination DEFAULT_MOUSE_ZOOM_UP_COMBINATION
        = new MouseKeyCombination(MouseButton.PRIMARY, KeyCodeCombination.SHORTCUT_DOWN);
    public static final MouseKeyCombination DEFAULT_MOUSE_ZOOM_DOWN_COMBINATION
        = new MouseKeyCombination(MouseButton.SECONDARY, KeyCodeCombination.SHORTCUT_DOWN);
    public static final MouseKeyCombination DEFAULT_MOUSE_ZOOM_DEFAULT_COMBINATION
        = new MouseKeyCombination(MouseButton.MIDDLE, KeyCodeCombination.SHORTCUT_DOWN);

    public static final MouseKeyCombination DEFAULT_MOUSE_POPULATION_TOGGLE_COMBINATION
        = new MouseKeyCombination(MouseButton.PRIMARY, true);

    public static final ScrollDirectionCombination DEFAULT_SCROLL_SPEED_UP_COMBINATION
        = new ScrollDirectionCombination
        (
            ScrollDirectionCombination.Direction.UP,
            KeyCodeCombination.ALT_DOWN
        );
    public static final ScrollDirectionCombination DEFAULT_SCROLL_SPEED_DOWN_COMBINATION
        = new ScrollDirectionCombination
        (
            ScrollDirectionCombination.Direction.DOWN,
            KeyCodeCombination.ALT_DOWN
        );

    public static final ScrollDirectionCombination DEFAULT_SCROLL_ZOOM_UP_COMBINATION
        = new ScrollDirectionCombination
        (
            ScrollDirectionCombination.Direction.UP,
            KeyCodeCombination.SHORTCUT_DOWN
        );
    public static final ScrollDirectionCombination DEFAULT_SCROLL_ZOOM_DOWN_COMBINATION
        = new ScrollDirectionCombination
        (
            ScrollDirectionCombination.Direction.DOWN,
            KeyCodeCombination.SHORTCUT_DOWN
        );

    public static final KeyCombination DEFAULT_STATE_TOGGLE_COMBINATION
        = new KeyCodeCombination(KeyCode.P);

    public static final KeyCombination DEFAULT_NEW_GAME_COMBINATION
        = new KeyCodeCombination(KeyCode.ESCAPE);
    public static final KeyCombination DEFAULT_GENERATION_SAVE_COMBINATION
        = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN);
    public static final KeyCombination DEFAULT_GENERATION_LOAD_COMBINATION
        = new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN);
    public static final KeyCombination DEFAULT_HELP_COMBINATION
        = new KeyCodeCombination(KeyCode.F1);

    public static final String PLAYING_STATUS = "PLAYING";
    public static final String PAUSED_STATUS = "PAUSED";

    public static final String WELCOME_TIP = "Press the play button to begin. "
        + "See the help section for the rules and key bindings info.";

    public static final String HELP_MSG_HEADER =
        "Info:\n"
        + "This is a basic Conway's Game of Life implementation.\n"
        + "\n"
        + "Rules:\n"
        + "Any live cell with fewer than two live neighbors dies.\n"
        + "Any live cell with two or three live neighbors lives on.\n"
        + "Any live cell with more than three live neighbors dies.\n"
        + "Any dead cell with exactly three live neighbors becomes a live cell.\n"
        + "\n";
    public static final String HELP_MSG_FOOTER =
        "\n"
        + "Misc:\n"
        + "nephest.com/projects/jhc-life\n"
        + "GPL Version 3\n"
        + "Copyright (C) 2018 Oleksandr Masniuk\n";


    private FileIO fileIO = new StandardFileIO();
    private ObjectTranslator<Generation> generationTranslator;

    private final ControlBindings<ControlType, KeyCombination> keyControl
        = new ControlBindings(ControlType.class);
    private final ControlBindings<ControlType, MouseKeyCombination> mouseControl
        = new ControlBindings(ControlType.class);
    private final ControlBindings<ControlType, ScrollDirectionCombination> scrollControl
        = new ControlBindings(ControlType.class);
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
        initControls();
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

    private void initControls()
    {
        initActions();
        initMouseControl();
        initScrollControl();
        initKeyControl();
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
            ControlType.GENERATION_LOAD,
            (x, y, zone)->{ generationLoad(x, y, zone); }
        );

        getControlActions().put
        (
            ControlType.GENERATION_SAVE,
            (x, y, zone)->{ generationSave(x, y, zone); }
        );

        getControlActions().put
        (
            ControlType.HELP,
            (x, y, zone)->{ help(x, y, zone); }
        );

        getControlActions().put
        (
            ControlType.STATE_TOGGLE,
            (x, y, zone)->{ toggleState(x, y, zone); }
        );

        getControlActions().put
        (
            ControlType.ZOOM_UP,
            (x, y, zone)->
            {
                changeZoom(x, y, zone, ZOOM_FACTOR_UP);
            }
        );

        getControlActions().put
        (
            ControlType.ZOOM_DOWN,
            (x, y, zone)->
            {
                changeZoom(x, y, zone, ZOOM_FACTOR_DOWN);
            }
        );

        getControlActions().put
        (
            ControlType.ZOOM_DEFAULT,
            (x, y, zone)->
            {
                changeZoom(x, y, zone, ZOOM_FACTOR_INIT);
            }
        );

        getControlActions().put
        (
            ControlType.SPEED_UP,
            (x, y, zone)->{ changeSpeed(x, y, zone, getSpeed() + SPEED_STEP); }
        );

        getControlActions().put
        (
            ControlType.SPEED_DOWN,
            (x, y, zone)->{ changeSpeed(x, y, zone, getSpeed() - SPEED_STEP); }
        );

        getControlActions().put
        (
            ControlType.SPEED_DEFAULT,
            (x, y, zone)->{ changeSpeed(x, y, zone, SPEED_INIT); }
        );

        getControlActions().put
        (
            ControlType.POPULAITON_TOGGLE,
            (x, y, zone)->{ togglePopulation((int)x, (int)y, zone); }
        );
    }

    private void initMouseControl()
    {
        getMouseControl().setBinding
        (
            ControlType.SPEED_UP,
            DEFAULT_MOUSE_SPEED_UP_COMBINATION
        );

        getMouseControl().setBinding
        (
            ControlType.SPEED_DOWN,
            DEFAULT_MOUSE_SPEED_DOWN_COMBINATION
        );

        getMouseControl().setBinding
        (
            ControlType.SPEED_DEFAULT,
            DEFAULT_MOUSE_SPEED_DEFAULT_COMBINATION
        );

        getMouseControl().setBinding
        (
            ControlType.ZOOM_UP,
            DEFAULT_MOUSE_ZOOM_UP_COMBINATION
        );

        getMouseControl().setBinding
        (
            ControlType.ZOOM_DOWN,
            DEFAULT_MOUSE_ZOOM_DOWN_COMBINATION
        );

        getMouseControl().setBinding
        (
            ControlType.ZOOM_DEFAULT,
            DEFAULT_MOUSE_ZOOM_DEFAULT_COMBINATION
        );

        getMouseControl().setBinding
        (
            ControlType.POPULAITON_TOGGLE,
            DEFAULT_MOUSE_POPULATION_TOGGLE_COMBINATION
        );
    }

    private void initScrollControl()
    {
        getScrollControl().setBinding
        (
            ControlType.SPEED_UP,
            DEFAULT_SCROLL_SPEED_UP_COMBINATION
        );

        getScrollControl().setBinding
        (
            ControlType.SPEED_DOWN,
            DEFAULT_SCROLL_SPEED_DOWN_COMBINATION
        );

        getScrollControl().setBinding
        (
            ControlType.ZOOM_UP,
            DEFAULT_SCROLL_ZOOM_UP_COMBINATION
        );

        getScrollControl().setBinding
        (
            ControlType.ZOOM_DOWN,
            DEFAULT_SCROLL_ZOOM_DOWN_COMBINATION
        );
    }

    private void initKeyControl()
    {
        getKeyControl().setBinding
        (
            ControlType.NEW_GAME,
            DEFAULT_NEW_GAME_COMBINATION
        );

        getKeyControl().setBinding
        (
            ControlType.GENERATION_LOAD,
            DEFAULT_GENERATION_LOAD_COMBINATION
        );

        getKeyControl().setBinding
        (
            ControlType.GENERATION_SAVE,
            DEFAULT_GENERATION_SAVE_COMBINATION
        );

        getKeyControl().setBinding
        (
            ControlType.HELP,
            DEFAULT_HELP_COMBINATION
        );

        getKeyControl().setBinding
        (
            ControlType.STATE_TOGGLE,
            DEFAULT_STATE_TOGGLE_COMBINATION
        );
    }

    private void initInfo()
    {
        initControlBindingsInfo();
        changeSpeed(SPEED_INIT);
        changeZoom(ZOOM_FACTOR_INIT);
        getView().setStatus(PAUSED_STATUS);
        getView().setTip(WELCOME_TIP);
    }

    private void initControlBindingsInfo()
    {
        getView().setControlBindingsInfo
        (
            DisplayableKeyCombination.toDisplayable(getKeyControl()),
            getMouseControl(),
            getScrollControl()
        );
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
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.ZOOM_UP)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onZoomDown()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.ZOOM_DOWN)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onZoomDefault()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.ZOOM_DEFAULT)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onSpeedUp()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.SPEED_UP)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onSpeedDown()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.SPEED_DOWN)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onSpeedDefault()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.SPEED_DEFAULT)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onStateToggle()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.STATE_TOGGLE)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onNewGame()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.NEW_GAME)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onGenerationSave()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.GENERATION_SAVE)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onGenerationLoad()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.GENERATION_LOAD)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
            }

            @Override
            public void onHelp()
            {
                getExecutor().execute
                (
                    ()->
                    {
                        getControlActions().get(ControlType.HELP)
                        .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL);
                    }
                );
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
        for (ControlType type : ControlType.values())
        {
            MouseKeyCombination bind = getMouseControl().getBinding(type);
            if (bind != null && bind.match(evt))
            {
                getControlActions().get(type).consume(evt.getX(), evt.getY(), zone);
                break;
            }
        }
    }

    private boolean mustConsumeEvent(MouseEvent evt, LifeView.Zone zone)
    {
        if (zone == LifeView.Zone.GENERATION) return false;

        boolean match = false;
        for (ControlType type : ControlType.values())
        {
            MouseKeyCombination bind = getMouseControl().getBinding(type);
            if (bind != null && bind.match(evt))
            {
                match = true;
                break;
            }
        }
        return match;
    }

    private boolean mustConsumeEvent(ScrollEvent evt, LifeView.Zone zone)
    {
        if (zone == LifeView.Zone.GENERATION) return false;

        boolean match = false;
        for (ControlType type : ControlType.values())
        {
            ScrollDirectionCombination bind = getScrollControl().getBinding(type);
            if (bind != null && bind.match(evt))
            {
                match = true;
                break;
            }
        }
        return match;
    }

    private boolean mustConsumeEvent(KeyEvent evt, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GLOBAL) return false;

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
        for (ControlType type : ControlType.values())
        {
            ScrollDirectionCombination bind = getScrollControl().getBinding(type);
            if (bind != null && bind.match(evt))
            {
                getControlActions().get(type).consume(evt.getX(), evt.getY(), zone);
                break;
            }
        }
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

    private void toggleState(double x, double y, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GLOBAL) return;
        if (getModel().isRunning())
        {
            pause();
        }
        else
        {
            play();
        }
    }

    private void pause()
    {
        getModel().stop();
        getView().setStatus(PAUSED_STATUS);
        getView().setTip(WELCOME_TIP);
    }

    private void play()
    {
        getModel().start();
        getView().setStatus(PLAYING_STATUS);
        getView().setTip("You can edit the population even while simulation is running");
    }

    private void newGame(double x, double y, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GLOBAL) return;
        pause();
        getMainController().setViewType(MainView.ViewType.MAIN_MENU);
    }

    private void generationSave(double x, double y, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GLOBAL) return;
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

    private void generationLoad(double x, double y, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GLOBAL) return;
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

    private void help(double x, double y, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GLOBAL) return;
        StringBuilder sb = new StringBuilder(HELP_MSG_HEADER);
        sb.append("Binds:\n")

        .append("Zoom+\t\t")
        .append(getMouseControl().getBinding(ControlType.ZOOM_UP).getDisplayText())
        .append(" | ")
        .append(getScrollControl().getBinding(ControlType.ZOOM_UP).getDisplayText())
        .append("\n")

        .append("Zoom-\t\t")
        .append(getMouseControl().getBinding(ControlType.ZOOM_DOWN).getDisplayText())
        .append(" | ")
        .append(getScrollControl().getBinding(ControlType.ZOOM_DOWN).getDisplayText())
        .append("\n")

        .append("Zoom default\t")
        .append(getMouseControl().getBinding(ControlType.ZOOM_DEFAULT).getDisplayText())
        .append("\n")
        .append("\n")

        .append("Speed+\t\t")
        .append(getMouseControl().getBinding(ControlType.SPEED_UP).getDisplayText())
        .append(" | ")
        .append(getScrollControl().getBinding(ControlType.SPEED_UP).getDisplayText())
        .append("\n")

        .append("Speed-\t\t")
        .append(getMouseControl().getBinding(ControlType.SPEED_DOWN).getDisplayText())
        .append(" | ")
        .append(getScrollControl().getBinding(ControlType.SPEED_DOWN).getDisplayText())
        .append("\n")

        .append("Speed default\t")
        .append(getMouseControl().getBinding(ControlType.SPEED_DEFAULT).getDisplayText())
        .append("\n")
        .append("\n")

        .append("Population\t")
        .append(getMouseControl().getBinding(ControlType.POPULAITON_TOGGLE).getDisplayText())
        .append("\n")

        .append("Play/Pause\t")
        .append(getKeyControl().getBinding(ControlType.STATE_TOGGLE).getDisplayText())
        .append("\n")

        .append("New game\t")
        .append(getKeyControl().getBinding(ControlType.NEW_GAME).getDisplayText())
        .append("\n")

        .append("Load game\t")
        .append(getKeyControl().getBinding(ControlType.GENERATION_LOAD).getDisplayText())
        .append("\n")

        .append("Save game\t")
        .append(getKeyControl().getBinding(ControlType.GENERATION_SAVE).getDisplayText())
        .append("\n")

        .append(HELP_MSG_FOOTER);
        getView().fireInfoAlert("Help", sb.toString());
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

    private Map<ControlType, EventConsumer<LifeView.Zone>> getControlActions()
    {
        return this.controlActions;
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

    public ControlBindings<ControlType, KeyCombination> getKeyControl()
    {
        return this.keyControl;
    }

    public ControlBindings<ControlType, MouseKeyCombination> getMouseControl()
    {
        return this.mouseControl;
    }

    public ControlBindings<ControlType, ScrollDirectionCombination> getScrollControl()
    {
        return this.scrollControl;
    }

    private Generation getLastGeneration()
    {
        return this.lastGeneration;
    }

    private void changeZoom(double x, double y, LifeView.Zone zone, double factor)
    {
        if (zone == LifeView.Zone.GLOBAL)
        {
            changeZoom(factor);
        }
        else if (zone == LifeView.Zone.GENERATION_CONTAINER)
        {
            changeZoom(factor, (int) x, (int) y);
        }
        //generation zoom event is not consumed is and passed to its container
    }

    private void changeZoom(double factor, int x, int y)
    {
        getView().setGenerationZoom(factor, x, y);
        getView().updateZoomInfo(ZOOM_FORMAT);
        getView().setTip("Hover over the play field to zoom a specific area");
    }

    private void changeZoom(double factor)
    {
        getView().setGenerationZoom(factor);
        getView().updateZoomInfo(ZOOM_FORMAT);
    }

    private void changeSpeed(double x, double y, LifeView.Zone zone, int speed)
    {
        if (zone == LifeView.Zone.GENERATION) return;
        changeSpeed(speed);
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

    private void togglePopulation(int x, int y, LifeView.Zone zone)
    {
        if (zone != LifeView.Zone.GENERATION) return;
        boolean pop = getLastGeneration().isPopulationAlive(x, y);
        getModel().setPopulation(x, y, !pop);
    }

    public int getSpeed()
    {
        return this.speed;
    }
}
