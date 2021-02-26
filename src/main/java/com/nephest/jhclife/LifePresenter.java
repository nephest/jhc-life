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
import com.nephest.jhclife.util.ObjectTranslator;
import javafx.scene.input.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public enum ControlType
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
        POPULATION_TOGGLE
    }

    public enum Tip
    {
        WELCOME,
        SPEED_CONTROL,
        ZOOM_CONTROL,
        ZOOM_PIVOT,
        POPULATION_TOGGLE,
    }

    private final Map<ControlType, EventConsumer<LifeView.Zone>> controlActions
        = new EnumMap<>(ControlType.class);

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

    public static final String CONTROL_NAME_SPLITTER = " ";
    public static final String CONTROL_PREFIX = "[";
    public static final String CONTROL_SUFFIX = "]";
    public static final String CONTROL_SPLITTER = " | ";

    public static final String PLAYING_STATUS = "PLAYING";
    public static final String PAUSED_STATUS = "PAUSED";

    public static final String HELP_MSG_HEADER =
        "Info:\n"
        + "This is a basic Conway's Game of Life implementation.\n"
        + "\n"
        + "Rules:\n"
        + "Any living cell with fewer than two living neighbors dies.\n"
        + "Any living cell with two or three living neighbors lives on.\n"
        + "Any living cell with more than three living neighbors dies.\n"
        + "Any dead cell with exactly three living neighbors becomes a living cell.\n"
        + "\n";
    public static final String HELP_MSG_FOOTER =
        "\n"
        + "Misc:\n"
        + "github.com/nephest/jhc-life\n"
        + "MIT License\n"
        + "Copyright (C) 2018-" + LocalDate.now().getYear() + " Oleksandr Masniuk\n";

    private final Map<Tip, String> tips = new EnumMap<>(Tip.class);

    private FileIO fileIO = new StandardFileIO();
    private ObjectTranslator<Generation> generationTranslator;

    private final ControlBindings<ControlType, KeyCombination> keyControl
        = new ControlBindings<>(ControlType.class);
    private final ControlBindings<ControlType, MouseKeyCombination> mouseControl
        = new ControlBindings<>(ControlType.class);
    private final ControlBindings<ControlType, ScrollDirectionCombination> scrollControl
        = new ControlBindings<>(ControlType.class);
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
        this.generationTranslator = new ObjectTranslator<>()
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
            ControlType.NEW_GAME, this::newGame
        );

        getControlActions().put
        (
            ControlType.GENERATION_LOAD, this::generationLoad
        );

        getControlActions().put
        (
            ControlType.GENERATION_SAVE, this::generationSave
        );

        getControlActions().put
        (
            ControlType.HELP, this::help
        );

        getControlActions().put
        (
            ControlType.STATE_TOGGLE, this::toggleState
        );

        getControlActions().put
        (
            ControlType.ZOOM_UP,
            (x, y, zone)-> changeZoom(x, y, zone, ZOOM_FACTOR_UP)
        );

        getControlActions().put
        (
            ControlType.ZOOM_DOWN,
            (x, y, zone)-> changeZoom(x, y, zone, ZOOM_FACTOR_DOWN)
        );

        getControlActions().put
        (
            ControlType.ZOOM_DEFAULT,
            (x, y, zone)-> changeZoom(x, y, zone, ZOOM_FACTOR_INIT)
        );

        getControlActions().put
        (
            ControlType.SPEED_UP,
            (x, y, zone)-> changeSpeed(x, y, zone, getSpeed() + SPEED_STEP)
        );

        getControlActions().put
        (
            ControlType.SPEED_DOWN,
            (x, y, zone)-> changeSpeed(x, y, zone, getSpeed() - SPEED_STEP)
        );

        getControlActions().put
        (
            ControlType.SPEED_DEFAULT,
            (x, y, zone)-> changeSpeed(x, y, zone, SPEED_INIT)
        );

        getControlActions().put
        (
            ControlType.POPULATION_TOGGLE,
            (x, y, zone)-> togglePopulation((int)x, (int)y, zone)
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
            ControlType.POPULATION_TOGGLE,
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
        initTips();
        changeSpeed(SPEED_INIT);
        changeZoom(ZOOM_FACTOR_INIT);
        getView().setStatus(PAUSED_STATUS);
        getView().setTip(getTip(Tip.WELCOME));
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

    private void initTips()
    {
        String welcomeTip =
            "Press the play button"
            + getControlBindingsString(ControlType.STATE_TOGGLE, "")
            + " to begin. "
            + "See the help"
            + getControlBindingsString(ControlType.HELP, "")
            + " section for the rules and key bindings info.";
        this.tips.put(Tip.WELCOME, welcomeTip);

        String populationTip =
            "You can edit the population"
            + getControlBindingsString(ControlType.POPULATION_TOGGLE, "")
            + " even while simulation is running";
        this.tips.put(Tip.POPULATION_TOGGLE, populationTip);

        String zoomControlTip =
            getControlBindingsString(ControlType.ZOOM_UP, "Zoom+")
            + " "
            + getControlBindingsString(ControlType.ZOOM_DOWN, "Zoom-")
            + " "
            + getControlBindingsString(ControlType.ZOOM_DEFAULT, "Zoom default");
        this.tips.put(Tip.ZOOM_CONTROL, zoomControlTip);

        String speedControlTip =
            getControlBindingsString(ControlType.SPEED_UP, "Speed+")
            + " "
            + getControlBindingsString(ControlType.SPEED_DOWN, "Speed-")
            + " "
            + getControlBindingsString(ControlType.SPEED_DEFAULT, "Speed default");
        this.tips.put(Tip.SPEED_CONTROL, speedControlTip);

        String zoomPivotTip = "Hover over the play field to zoom a specific area";
        this.tips.put(Tip.ZOOM_PIVOT, zoomPivotTip);
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
                    ()-> getControlActions().get(ControlType.ZOOM_UP)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onZoomDown()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.ZOOM_DOWN)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onZoomDefault()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.ZOOM_DEFAULT)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onSpeedUp()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.SPEED_UP)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onSpeedDown()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.SPEED_DOWN)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onSpeedDefault()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.SPEED_DEFAULT)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onStateToggle()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.STATE_TOGGLE)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onNewGame()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.NEW_GAME)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onGenerationSave()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.GENERATION_SAVE)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onGenerationLoad()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.GENERATION_LOAD)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
                );
            }

            @Override
            public void onHelp()
            {
                getExecutor().execute
                (
                    ()-> getControlActions().get(ControlType.HELP)
                    .consume(Double.NaN, Double.NaN, LifeView.Zone.GLOBAL)
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
        getView().setTip(getTip(Tip.WELCOME));
    }

    private void play()
    {
        getModel().start();
        getView().setStatus(PLAYING_STATUS);
        getView().setTip(getTip(Tip.POPULATION_TOGGLE));
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
                ()-> getExecutor().execute(()->doSaveGeneration(file, generation)),
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
        String
            sb =
            HELP_MSG_HEADER
                + "Binds:\n"
                + "Zoom+\t\t"
                + getControlBindingsString(ControlType.ZOOM_UP)
                + "\n"
                + "Zoom-\t\t"
                + getControlBindingsString(ControlType.ZOOM_DOWN)
                + "\n"
                + "Zoom default\t"
                + getControlBindingsString(ControlType.ZOOM_DEFAULT)
                + "\n"
                + "\n"
                + "Speed+\t\t"
                + getControlBindingsString(ControlType.SPEED_UP)
                + "\n"
                + "Speed-\t\t"
                + getControlBindingsString(ControlType.SPEED_DOWN)
                + "\n"
                + "Speed default\t"
                + getControlBindingsString(ControlType.SPEED_DEFAULT)
                + "\n"
                + "\n"
                + "Population\t"
                + getControlBindingsString(ControlType.POPULATION_TOGGLE)
                + "\n"
                + "Play/Pause\t"
                + getControlBindingsString(ControlType.STATE_TOGGLE)
                + "\n"
                + "New game\t"
                + getControlBindingsString(ControlType.NEW_GAME)
                + "\n"
                + "Load game\t"
                + getKeyControl().getBinding(ControlType.GENERATION_LOAD).getDisplayText()
                + "\n"
                + "Save game\t"
                + getKeyControl().getBinding(ControlType.GENERATION_SAVE).getDisplayText()
                + "\n"
                + HELP_MSG_FOOTER;
        getView().fireInfoAlert("Help", sb);
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
        getView().setTip(getTip(Tip.ZOOM_PIVOT));
    }

    private void changeZoom(double factor)
    {
        getView().setGenerationZoom(factor);
        getView().updateZoomInfo(ZOOM_FORMAT);
        getView().setTip(getTip(Tip.ZOOM_CONTROL));
    }

    private void changeSpeed(double x, double y, LifeView.Zone zone, int speed)
    {
        if (zone == LifeView.Zone.GENERATION) return;
        changeSpeed(speed);
        getView().setTip(getTip(Tip.SPEED_CONTROL));
    }

    private void changeSpeed(int speed)
    {
        speed = Math.max(speed, 1);
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

    private String getControlBindingsString(ControlType ctrl)
    {
        return ControlBindings.calculateBindingsString
        (
            CONTROL_SPLITTER,
            ctrl,
            DisplayableKeyCombination.toDisplayable(getKeyControl()),
            getMouseControl(),
            getScrollControl()
        );
    }

    private String getControlBindingsString(ControlType ctrl, String name)
    {
        return ControlBindings.calculateControlName
        (
            name,
            CONTROL_NAME_SPLITTER,
            CONTROL_PREFIX,
            CONTROL_SPLITTER,
            CONTROL_SUFFIX,
            ctrl,
            DisplayableKeyCombination.toDisplayable(getKeyControl()),
            getMouseControl(),
            getScrollControl()
        );
    }

    private String getTip(Tip tip)
    {
        return this.tips.get(tip);
    }

    public int getSpeed()
    {
        return this.speed;
    }
}
