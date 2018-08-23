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

import com.nephest.jhclife.io.FileIO;
import com.nephest.jhclife.util.ObjectTranslator;

import java.io.*;
import java.nio.file.Path;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import javafx.event.*;
import javafx.scene.input.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

public class LifePresenterTest
{

    public static final String ZOOM_FORMAT="%06.2f";
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

    private LifeView<?> viewMock;
    private ClassicLifeModel modelMock;
    private MainController controllerMock;
    private Executor executorMock;
    private LifeViewListener listener;

    private LifePresenter presenter;
    private FileIO fileIOMock;
    private ObjectTranslator<Generation> generationTranslatorMock;

    @Before
    public void init()
    {
        this.viewMock = mock(LifeView.class);
        this.modelMock = mock(ClassicLifeModel.class);
        this.controllerMock = mock(MainController.class);
        this.executorMock = mock(Executor.class);
        this.fileIOMock = mock(FileIO.class);
        this.generationTranslatorMock = mock(ObjectTranslator.class);

        this.presenter = new LifePresenter
        (
            this.viewMock,
            this.modelMock,
            this.controllerMock,
            this.executorMock
        );
        this.presenter.setFileIO(this.fileIOMock);
        this.presenter.setGenerationTranslator(this.generationTranslatorMock);

        this.listener = getListener();

        verifyInit();
    }

    private void verifyInit()
    {
        verify(this.viewMock).setGenerationZoom(LifePresenter.ZOOM_FACTOR_INIT);
        verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);

        long period = 1_000_000_000 / LifePresenter.SPEED_INIT;
        verify(this.modelMock).setGenerationLifeTime(period, TimeUnit.NANOSECONDS);
        verify(this.viewMock)
            .setSpeedInfo(String.format(SPEED_FORMAT, LifePresenter.SPEED_INIT));
        verify(this.viewMock).setStatus(LifePresenter.PAUSED_STATUS);

        //clear for easier zoom/speed testing
        //need to specify the exact invocation count or use ordering otherwise
        verifyNoMoreInteractions(this.viewMock);
        clearInvocations(this.viewMock);
        verifyNoMoreInteractions(this.modelMock);
        clearInvocations(this.modelMock);
    }

    @Test
    public void testMouseEventTogglePopulaiton()
    {
        double x = 1.0;
        double y = 2.0;
        boolean alive = false;

        Generation generation = mock(Generation.class);
        when(generation.isPopulationAlive( (int)x, (int)y)).thenReturn(alive);
        when(modelMock.getLastGeneration()).thenReturn(generation);

        this.listener.readyForNextFrame(); //generation must be rendered first

        MouseEvent evt = new MouseEvent
        (
            MouseEvent.MOUSE_CLICKED, x, y, x, y, MouseButton.PRIMARY, 1,
            false, false, false, false, //shift, ctrl, alt, meta
            false, false, false, //primary, mid, secondary
            false, false, true, //synthesized, popup, still
            null
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(evt, LifeView.Zone.GENERATION);
        verifyRunInBackground(captor);

        verify(this.modelMock).setPopulation( (int)x, (int)y, !alive);
        assertFalse(evt.isConsumed());
    }

    @Test
    public void testMouseEventTogglePopulaitonDrag()
    {
        double x = 1.0;
        double y = 2.0;
        boolean alive = false;

        Generation generation = mock(Generation.class);
        when(generation.isPopulationAlive( (int)x, (int)y)).thenReturn(alive);
        when(modelMock.getLastGeneration()).thenReturn(generation);

        this.listener.readyForNextFrame(); //generation must be rendered first

        MouseEvent evt = new MouseEvent
        (
            MouseEvent.MOUSE_CLICKED, x, y, x, y, MouseButton.PRIMARY, 1,
            false, false, false, false, //shift, ctrl, alt, meta
            false, false, false, //primary, mid, secondary
            false, false, false, //synthesized, popup, still
            null
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(evt, LifeView.Zone.GENERATION);
        verifyRunInBackground(captor);

        verify(this.modelMock, never()).setPopulation( (int)x, (int)y, !alive);
        assertFalse(evt.isConsumed());
    }

    @Test
    public void testMouseEventSpeedDefault()
    {
        testMouseEventSpeed(MouseButton.MIDDLE, LifePresenter.SPEED_INIT);
    }

    @Test
    public void testMouseEventSpeedDown()
    {
        testMouseEventSpeed
        (
            MouseButton.SECONDARY,
            this.presenter.getSpeed() - LifePresenter.SPEED_STEP
        );
    }

    @Test
    public void testMouseEventSpeedUp()
    {
        testMouseEventSpeed
        (
            MouseButton.PRIMARY,
            this.presenter.getSpeed() + LifePresenter.SPEED_STEP
        );
    }

    private void testMouseEventSpeed(MouseButton button, int targetSpeed)
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            testMouseEventSpeed(button, targetSpeed, zone);
        }
    }

    private void testMouseEventSpeed(MouseButton button, int targetSpeed, LifeView.Zone zone)
    {
        if (zone == LifeView.Zone.GENERATION) return;
        MouseEvent evt = new MouseEvent
        (
            MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, 1,
            false, false, true, false, //shift, ctrl, alt, meta
            false, false, false, //primary, mid, secondary
            false, false, false,
            null
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(evt, zone);
        //consume in the GUI thread to properly stop the propagation chain
        if (zone != LifeView.Zone.GENERATION) assertTrue(evt.isConsumed());
        verifyRunInBackground(captor);

        long period = 1_000_000_000 / targetSpeed;
        verify(this.modelMock).setGenerationLifeTime(period, TimeUnit.NANOSECONDS);
        verify(this.viewMock).setSpeedInfo(String.format(SPEED_FORMAT, targetSpeed));
    }

    @Test
    public void testMouseEventZoomDefault()
    {
        testMouseEventZoom(MouseButton.MIDDLE, LifePresenter.ZOOM_FACTOR_INIT);
    }

    @Test
    public void testMouseEventZoomDown()
    {
        testMouseEventZoom(MouseButton.SECONDARY, LifePresenter.ZOOM_FACTOR_DOWN);
    }

    @Test
    public void testMouseEventZoomUp()
    {
        testMouseEventZoom(MouseButton.PRIMARY, LifePresenter.ZOOM_FACTOR_UP);
    }

    private void testMouseEventZoom(MouseButton button, double factor)
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            testMouseEventZoom(button, factor, zone);
        }
    }

    private void testMouseEventZoom(MouseButton button, double factor, LifeView.Zone zone)
    {
        int x = 11;
        int y = 232;

        MouseEvent evt = new MouseEvent
        (
            MouseEvent.MOUSE_CLICKED, x, y, x, y, button, 1,
            false, true, false, false, //shift, ctrl, alt, meta
            false, false, false, //primary, mid, secondary
            false, false, false,
            null
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(evt, zone);
        //consume in the GUI thread to properly stop the propagation chain
        if (zone != LifeView.Zone.GENERATION) assertTrue(evt.isConsumed());
        verifyRunInBackground(captor);

        switch(zone)
        {
            case GENERATION_CONTAINER:
                verify(this.viewMock).setGenerationZoom(factor, x, y);
                verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);
                break;
            case GLOBAL:
                verify(this.viewMock).setGenerationZoom(factor);
                verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);
                break;
            case GENERATION:
                //the generation handler must pass it to the container handler
                verifyNoMoreInteractions(this.viewMock);
                break;
        }
    }

    @Test
    public void testScrollEventSpeedDown()
    {
        testScrollEventSpeed
        (
            true,
            this.presenter.getSpeed() - LifePresenter.SPEED_STEP
        );
    }

    @Test
    public void testScrollEventSpeedUp()
    {
        testScrollEventSpeed
        (
            false,
            this.presenter.getSpeed() + LifePresenter.SPEED_STEP
        );
    }

    private void testScrollEventSpeed(boolean down, int targetSpeed)
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            testScrollEventSpeed(down, targetSpeed, zone);
        }
    }

    private void testScrollEventSpeed(boolean down, int targetSpeed, LifeView.Zone zone)
    {
        if (zone == LifeView.Zone.GENERATION) return;
        //the sign is the ONLY thing that matters
        double deltaY = down ? -32.0 : 1.0;
        ScrollEvent evt = new ScrollEvent
        (
            ScrollEvent.SCROLL, 0, 0, 0, 0, //type, x, y
            false, false, true, false, //shift, ctrl, alt, meta
            false, false,
            0, deltaY, 0, 0, //deltaX, deltaY
            null, 0, null, 0, 1, null
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onScrollEvent(evt, zone);
        //consume in the GUI thread to properly stop the propagation chain
        if (zone != LifeView.Zone.GENERATION) assertTrue(evt.isConsumed());
        verifyRunInBackground(captor);
        long period = 1_000_000_000 / targetSpeed;
        verify(this.modelMock).setGenerationLifeTime(period, TimeUnit.NANOSECONDS);
        verify(this.viewMock).setSpeedInfo(String.format(SPEED_FORMAT, targetSpeed));
    }

    @Test
    public void testScrollEventZoomDown()
    {
        testScrollEventZoom(true, LifePresenter.ZOOM_FACTOR_DOWN);
    }

    @Test
    public void testScrollEventZoomUp()
    {
        testScrollEventZoom(false, LifePresenter.ZOOM_FACTOR_UP);
    }

    private void testScrollEventZoom(boolean down, double factor)
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            testScrollEventZoom(down, factor, zone);
        }
    }

    private void testScrollEventZoom(boolean down, double factor, LifeView.Zone zone)
    {
        int x = 11;
        int y = 232;

        //the sign is the ONLY thing that matters
        double deltaY = down ? -32.0 : 1.0;
        ScrollEvent evt = new ScrollEvent
        (
            ScrollEvent.SCROLL, x, y, 0, 0, //type, x, y
            false, true, false, false, //shift, ctrl, alt, meta
            false, false,
            0, deltaY, 0, 0, //deltaX, deltaY
            null, 0, null, 0, 1, null
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onScrollEvent(evt, zone);
        //consume in the GUI thread to properly stop the propagation chain
        if (zone != LifeView.Zone.GENERATION) assertTrue(evt.isConsumed());
        verifyRunInBackground(captor);

        switch(zone)
        {
            case GENERATION_CONTAINER:
                verify(this.viewMock).setGenerationZoom(factor, x, y);
                verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);
                break;
            case GLOBAL:
                verify(this.viewMock).setGenerationZoom(factor);
                verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);
                break;
            case GENERATION:
                //the generation handler must pass it to the container handler
                verifyNoMoreInteractions(this.viewMock);
                break;
        }
    }

    @Test
    public void testKeyTogglePlay()
    {
        Set<KeyCode> keys = new HashSet();
        keys.add(PLAY_TOGGLE);
        keys.add(PLAY_TOGGLE_ALT);
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            for (KeyCode code : keys)
            {
                init();
                testKeyTogglePlay(code, true, zone);
                init();
                testKeyTogglePlay(code, false, zone);
            }
        }
    }

    private void testKeyTogglePlay(KeyCode code, boolean running, LifeView.Zone zone)
    {
        LifeViewListener spy = spy(this.listener);
        this.presenter.setListener(spy);
        when(this.modelMock.isRunning()).thenReturn(running);

        KeyEvent evt = new KeyEvent
        (
            KeyEvent.KEY_PRESSED, "", "", code, //type, char, text, code
            false, false, false, false //shift, ctrl, alt, meta
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        spy.onKeyEvent(evt, zone);

        if (zone == LifeView.Zone.GLOBAL)
        {
            assertTrue(evt.isConsumed());
        }
        else
        {
            assertFalse(evt.isConsumed());
        }

        verifyRunInBackground(captor);

        if (zone != LifeView.Zone.GLOBAL)
        {
            verify(spy, never()).onPause();
            verify(spy, never()).onPlay();
        }
        else if(running)
        {
            verify(spy).onPause();
        }
        else
        {
            verify(spy).onPlay();
        }
    }

    private void testKeyNewGame(KeyCode code, LifeView.Zone zone)
    {
        LifeViewListener spy = spy(this.listener);
        this.presenter.setListener(spy);

        KeyEvent evt = new KeyEvent
        (
            KeyEvent.KEY_PRESSED, "", "", code, //type, char, text, code
            false, false, false, false //shift, ctrl, alt, meta
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        spy.onKeyEvent(evt, zone);

        //consume before handling
        if (zone == LifeView.Zone.GLOBAL)
        {
            assertTrue(evt.isConsumed());
        }
        else
        {
            assertFalse(evt.isConsumed());
        }
        verifyRunInBackground(captor);

        if (zone == LifeView.Zone.GLOBAL)
        {
            verify(spy).onNewGame();
        }
        else
        {
            verify(spy, never()).onNewGame();
        }
    }

    @Test
    public void testKeyNewGame()
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            testKeyNewGame(NEW_GAME, zone);
        }
    }

    private LifeViewListener testKeyGeneration(KeyCode code, LifeView.Zone zone)
    {
        LifeViewListener spy = spy(this.listener);
        this.presenter.setListener(spy);

        KeyEvent evt = new KeyEvent
        (
            KeyEvent.KEY_PRESSED, "", "", code, //type, char, text, code
            false, true, false, false //shift, ctrl, alt, meta
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        spy.onKeyEvent(evt, zone);

        if (zone == LifeView.Zone.GLOBAL)
        {
            assertTrue(evt.isConsumed());
        }
        else
        {
            assertFalse(evt.isConsumed());
        }
        verifyRunInBackground(captor);

        return spy;
    }

    @Test
    public void testKeyGenerationSave()
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            LifeViewListener spy = testKeyGeneration(GENERATION_SAVE, zone);
            if (zone != LifeView.Zone.GLOBAL)
            {
                verify(spy, never()).onGenerationSave();
            }
            else
            {
                verify(spy).onGenerationSave();
            }
        }
    }

    @Test
    public void testKeyGenerationLoad()
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            LifeViewListener spy = testKeyGeneration(GENERATION_LOAD, zone);
            if (zone != LifeView.Zone.GLOBAL)
            {
                verify(spy, never()).onGenerationLoad();
            }
            else
            {
                verify(spy).onGenerationLoad();
            }
        }
    }

    @Test
    public void testZoomUp()
    {
        this.listener.onZoomUp();
        testZoomViewControl(LifePresenter.ZOOM_FACTOR_UP);
    }

    @Test
    public void testZoomDown()
    {
        this.listener.onZoomDown();
        testZoomViewControl(LifePresenter.ZOOM_FACTOR_DOWN);
    }

    @Test
    public void testZoomDefault()
    {
        this.listener.onZoomDefault();
        testZoomViewControl(LifePresenter.ZOOM_FACTOR_INIT);
    }

    private void testZoomViewControl(double factor)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verifyRunInBackground(captor);
        verify(this.viewMock).setGenerationZoom(factor);
        verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);
    }

    @Test
    public void testSpeedUp()
    {
        this.listener.onSpeedUp();
        testSpeedViewControl(this.presenter.getSpeed() + LifePresenter.SPEED_STEP);
    }

    @Test
    public void testSpeedDown()
    {
        this.listener.onSpeedDown();
        testSpeedViewControl(this.presenter.getSpeed() - LifePresenter.SPEED_STEP);
    }

    @Test
    public void testSpeedDefault()
    {
        this.listener.onSpeedDefault();
        testSpeedViewControl(LifePresenter.SPEED_INIT);
    }

    private void testSpeedViewControl(int targetSpeed)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verifyRunInBackground(captor);

        long period = 1_000_000_000 / targetSpeed;
        verify(this.modelMock).setGenerationLifeTime(period, TimeUnit.NANOSECONDS);
        verify(this.viewMock).setSpeedInfo(String.format(SPEED_FORMAT, targetSpeed));
    }

    @Test
    public void testPause()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onPause();
        verifyRunInBackground(captor);
        verify(this.modelMock).stop();
        verify(this.viewMock).setStatus(LifePresenter.PAUSED_STATUS);
    }

    @Test
    public void testPlay()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onPlay();
        verifyRunInBackground(captor);
        verify(this.modelMock).start();
        verify(this.viewMock).setStatus(LifePresenter.PLAYING_STATUS);
    }

    @Test
    public void testNewGame()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onNewGame();
        verifyRunInBackground(captor);
        InOrder inOrder = inOrder(this.modelMock, this.controllerMock);
        inOrder.verify(this.modelMock).stop();
        inOrder.verify(this.controllerMock).setViewType(MainView.ViewType.MAIN_MENU);
    }

    private void testGenerationSave
    (
        ArgumentCaptor<Runnable> captor,
        File file,
        byte[] translatedBytes
    )
    {
        ArgumentCaptor<Consumer<List<File>>> consumerCaptor
            = ArgumentCaptor.forClass(Consumer.class);

        /*
            the next generation might be rendered while the user is selecting
            a save file destination. ignore it, save the generation that was
            rendered when the user started the saving process.
        */
        Generation generation = mock(Generation.class);
        Generation nextGeneration = mock(Generation.class);
        when(modelMock.getLastGeneration()).thenReturn(generation).thenReturn(nextGeneration);
        when(this.generationTranslatorMock.toByteArray(generation))
            .thenReturn(translatedBytes);

        Path filePath = mock(Path.class);
        when(file.toPath()).thenReturn(filePath);

        //render the generation so we have something to save
        this.listener.readyForNextFrame();

        //open a file selection dialog
        this.listener.onGenerationSave();
        verifyRunInBackground(captor);
        verify(this.viewMock).selectFile
        (
            eq(ViewBase.FileSelectionMode.SAVE),
            any(),
            any(),
            consumerCaptor.capture()
        );

        //save if a file was selected
        List<File> selectedFiles = new ArrayList();
        selectedFiles.add(file);
        consumerCaptor.getValue().accept(selectedFiles);
        verifyRunInBackground(captor, 2);
    }

    @Test
    public void testGenerationSaveNewFile()
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        byte[] translatedBytes = new byte[]{1, 5, 7, 0, 2, 4, 2, 6, 1, 1};
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);

        testGenerationSave(captor, file, translatedBytes);
        verify(this.fileIOMock).write(file.toPath(), translatedBytes);
    }

    @Test
    public void testGenerationSaveExistingFile()
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        byte[] translatedBytes = new byte[]{1, 5, 7, 0, 2, 4, 2, 6, 1, 1};
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);

        testGenerationSave(captor, file, translatedBytes);

        //ask for the confirmation
        verify(this.viewMock).fireConfirmationAlert
        (
            any(),
            any(),
            captor.capture(),
            any()
        );

        //save if confirmed by the user
        captor.getValue().run();
        verifyRunInBackground(captor, 3);
        verify(this.fileIOMock).write(file.toPath(), translatedBytes);
    }

    @Test
    public void testGenerationSaveError()
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        byte[] translatedBytes = new byte[]{1, 5, 7, 0, 2, 4, 2, 6, 1, 1};
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);
        doThrow(new IOException()).when(this.fileIOMock).write(any(), any());

        testGenerationSave(captor, file, translatedBytes);

        verify(this.viewMock).fireErrorAlert(eq("Generation saving failed"), any());
    }

    @Test
    public void testGenerationSaveNothingToSave()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onGenerationSave();
        verifyRunInBackground(captor);

        verify(this.viewMock).fireErrorAlert(eq("No generation to save"), any());
    }

    private void testGenerationLoad
    (
        ArgumentCaptor<Runnable> captor,
        File file,
        Generation generation
    )
    throws IOException
    {
        ArgumentCaptor<Consumer<List<File>>> consumerCaptor
            = ArgumentCaptor.forClass(Consumer.class);

        when(this.generationTranslatorMock.fromByteArray(any()))
            .thenReturn(generation);
        Path filePath = mock(Path.class);
        when(file.toPath()).thenReturn(filePath);

        this.listener.onGenerationLoad();
        verifyRunInBackground(captor);

        //show the file selection dialog
        verify(this.viewMock).selectFile
        (
            eq(ViewBase.FileSelectionMode.SELECT_SINGLE),
            any(),
            any(),
            consumerCaptor.capture()
        );

        //load if a file was selected
        List<File> files = new ArrayList();
        files.add(file);
        consumerCaptor.getValue().accept(files);
        verifyRunInBackground(captor, 2);
    }

    @Test
    public void testGenerationLoadExistingFile()
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Generation generation = mock(Generation.class);
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);

        testGenerationLoad(captor, file, generation);

        verify(this.modelMock).setGeneration(generation);
    }

    @Test
    public void testGenerationLoadNonExistingFile()
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Generation generation = mock(Generation.class);
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);

        testGenerationLoad(captor, file, generation);

        verify(this.viewMock).fireErrorAlert(eq("Generation loading failed"), any());
    }

    @Test
    public void testGenerationLoadError()
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Generation generation = mock(Generation.class);
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        doThrow(new IOException()).when(this.fileIOMock).readAllBytes(any());

        testGenerationLoad(captor, file, generation);

        verify(this.viewMock).fireErrorAlert(eq("Generation loading failed"), any());
    }

    @Test
    public void testHelp()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onHelp();
        verifyRunInBackground(captor);
        verify(this.viewMock).fireInfoAlert("Help", HELP_MSG);
    }

    @Test
    public void testReadyForNextFrame()
    {
        Generation generation = mock(Generation.class);
        when(generation.getId()).thenReturn(10l);
        when(generation.getGenerationNumber()).thenReturn(2l);
        when(this.modelMock.getLastGeneration()).thenReturn(generation);

        //the rendering request is running in the rendering thread to avoid missed frames
        this.listener.readyForNextFrame();
        this.listener.readyForNextFrame();

        //do not render the same generation more than once
        verify(this.viewMock, times(1)).render(generation);
    }

    private LifeViewListener getListener()
    {
        ArgumentCaptor<LifeViewListener> captor
            = ArgumentCaptor.forClass(LifeViewListener.class);
        verify(this.viewMock).setListener(captor.capture());
        return captor.getValue();
    }

    //verify that a Runnable is scheduled to execute in a bg thread, then run it
    private void verifyRunInBackground(ArgumentCaptor<Runnable> runCaptor, int times)
    {
        verify(executorMock, times(times)).execute(runCaptor.capture());
        runCaptor.getValue().run();
    }

    private void verifyRunInBackground(ArgumentCaptor<Runnable> runCaptor)
    {
        verifyRunInBackground(runCaptor, 1);
    }

}
