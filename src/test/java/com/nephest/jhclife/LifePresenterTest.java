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

import com.nephest.jhclife.io.FileIO;
import com.nephest.jhclife.util.ObjectTranslator;
import javafx.scene.input.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Mockito.*;

public class LifePresenterTest
{

    public static final String ZOOM_FORMAT="%06.2f";
    public static final String SPEED_FORMAT="%03d";

    public static final KeyCode PLAY_TOGGLE = KeyCode.P;

    public static final KeyCode NEW_GAME = KeyCode.ESCAPE;
    public static final KeyCode GENERATION_SAVE = KeyCode.S;
    public static final KeyCode GENERATION_LOAD = KeyCode.O;
    public static final KeyCode HELP = KeyCode.F1;

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

    private LifeView<?> viewMock;
    private ClassicLifeModel modelMock;
    private MainController controllerMock;
    private Executor executorMock;
    private LifeViewListener listener;

    private LifePresenter presenter;
    private FileIO fileIOMock;
    private ObjectTranslator<Generation> generationTranslatorMock;

    @BeforeEach
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
        verify(this.viewMock, atLeastOnce()).setTip(any());
        verify(this.viewMock).setControlBindingsInfo(any());

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
    public void testKeyToggle()
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            init();
            testKeyToggle(PLAY_TOGGLE, zone, true);
            init();
            testKeyToggle(PLAY_TOGGLE, zone, false);
        }
    }

    private void testKeyToggle(KeyCode code, LifeView.Zone zone, boolean running)
    {
        when(this.modelMock.isRunning()).thenReturn(running);

        KeyEvent evt = new KeyEvent
        (
            KeyEvent.KEY_PRESSED, "", "", code, //type, char, text, code
            false, false, false, false //shift, ctrl, alt, meta
        );

        Runnable trigger = ()->this.listener.onKeyEvent(evt, zone);
        if(zone == LifeView.Zone.GLOBAL)
        {
            if (running)
            {
                testToggleStatePauseAction(trigger, 1);
            }
            else
            {
                testToggleStatePlayAction(trigger, 1);
            }
        }
    }

    @Test
    public void testKeyNewGame()
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            KeyEvent evt = new KeyEvent
            (
                KeyEvent.KEY_PRESSED, "", "", NEW_GAME, //type, char, text, code
                false, false, false, false //shift, ctrl, alt, meta
            );
            Runnable trigger = ()->this.listener.onKeyEvent(evt, zone);
            init();
            if(zone == LifeView.Zone.GLOBAL)
            {
                testNewGame(trigger, 1);
            }
            else
            {
                testNewGame(trigger, 0);
            }
        }
    }

    @Test
    public void testKeyGenerationSave()
    throws IOException
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            KeyEvent evt = new KeyEvent
            (
                KeyEvent.KEY_PRESSED, "", "", GENERATION_SAVE, //type, char, text, code
                false, true, false, false //shift, ctrl, alt, meta
            );
            Runnable trigger = ()->this.listener.onKeyEvent(evt, zone);
            init();
            if(zone == LifeView.Zone.GLOBAL)
            {
                testGenerationSave(trigger, 1);
            }
            else
            {
                testGenerationSave(trigger, 0);
            }
        }
    }

    @Test
    public void testKeyGenerationLoad()
    throws IOException
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            KeyEvent evt = new KeyEvent
            (
                KeyEvent.KEY_PRESSED, "", "", GENERATION_LOAD, //type, char, text, code
                false, true, false, false //shift, ctrl, alt, meta
            );
            Runnable trigger = ()->this.listener.onKeyEvent(evt, zone);
            init();
            if(zone == LifeView.Zone.GLOBAL)
            {
                testGenerationLoad(trigger, 1);
            }
            else
            {
                testGenerationLoad(trigger, 0);
            }
        }
    }

    @Test
    public void testKeyHelp()
    {
        for (LifeView.Zone zone : LifeView.Zone.values())
        {
            KeyEvent evt = new KeyEvent
            (
                KeyEvent.KEY_PRESSED, "", "", HELP, //type, char, text, code
                false, false, false, false //shift, ctrl, alt, meta
            );
            Runnable trigger = ()->this.listener.onKeyEvent(evt, zone);
            init();
            if(zone == LifeView.Zone.GLOBAL)
            {
                testHelp(trigger, 1);
            }
            else
            {
                testHelp(trigger, 0);
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

    private void testToggleStatePlayAction(Runnable trigger, int times)
    {
        when(this.modelMock.isRunning()).thenReturn(false);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);
        verify(this.modelMock, times(times)).start();
        verify(this.viewMock, times(times)).setStatus(LifePresenter.PLAYING_STATUS);
    }

    private void testToggleStatePauseAction(Runnable trigger, int times)
    {
        when(this.modelMock.isRunning()).thenReturn(true);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);
        verify(this.modelMock, times(times)).stop();
        verify(this.viewMock, times(times)).setStatus(LifePresenter.PAUSED_STATUS);
    }

    @Test
    public void testListenerToggleStatePlay()
    {
        testToggleStatePlayAction( ()->this.listener.onStateToggle(), 1 );
    }

    @Test
    public void testListenerToggleStatePause()
    {
        testToggleStatePauseAction( ()->this.listener.onStateToggle(), 1 );
    }

    private void testNewGame(Runnable trigger, int times)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);
        InOrder inOrder = inOrder(this.modelMock, this.controllerMock);
        inOrder.verify(this.modelMock, times(times)).stop();
        inOrder.verify(this.controllerMock, times(times)).setViewType(MainView.ViewType.MAIN_MENU);
    }

    @Test
    public void testListenerNewGame()
    {
        testNewGame( ()->this.listener.onNewGame(), 1 );
    }

    private void testGenerationSave
    (
        ArgumentCaptor<Runnable> captor,
        Runnable trigger,
        int times,
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
        trigger.run();
        verifyRunInBackground(captor);
        verify(this.viewMock, times(times)).selectFile
        (
            eq(ViewBase.FileSelectionMode.SAVE),
            any(),
            any(),
            consumerCaptor.capture()
        );
        if (times == 0) return;

        //save if a file was selected
        List<File> selectedFiles = new ArrayList<>();
        selectedFiles.add(file);
        consumerCaptor.getValue().accept(selectedFiles);
        verifyRunInBackground(captor, 2);
    }

    private void testGenerationSaveNewFile(Runnable trigger, int times)
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        byte[] translatedBytes = new byte[]{1, 5, 7, 0, 2, 4, 2, 6, 1, 1};
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);

        testGenerationSave(captor, trigger, times, file, translatedBytes);
        verify(this.fileIOMock, times(times)).write(file.toPath(), translatedBytes);
    }

    private void testGenerationSaveExistingFile(Runnable trigger, int times)
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        byte[] translatedBytes = new byte[]{1, 5, 7, 0, 2, 4, 2, 6, 1, 1};
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);

        testGenerationSave(captor, trigger, times, file, translatedBytes);

        //ask for the confirmation
        verify(this.viewMock, times(times)).fireConfirmationAlert
        (
            any(),
            any(),
            captor.capture(),
            any()
        );

        //save if confirmed by the user
        captor.getValue().run();
        verifyRunInBackground(captor, times == 0 ? 1 : 3);
        verify(this.fileIOMock, times(times)).write(file.toPath(), translatedBytes);
    }

    private void testGenerationSaveError(Runnable trigger, int times)
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        byte[] translatedBytes = new byte[]{1, 5, 7, 0, 2, 4, 2, 6, 1, 1};
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);
        doThrow(new IOException()).when(this.fileIOMock).write(any(), any());

        testGenerationSave(captor, trigger, times, file, translatedBytes);

        verify(this.viewMock, times(times)).fireErrorAlert(eq("Generation saving failed"), any());
    }

    private void testGenerationSaveNothingToSave(Runnable trigger, int times)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);

        verify(this.viewMock, times(times)).fireErrorAlert(eq("No generation to save"), any());
    }

    private void testGenerationSave(Runnable trigger, int times)
    throws IOException
    {
        testGenerationSaveNewFile(trigger, times);
        init();
        testGenerationSaveExistingFile(trigger, times);
        init();
        testGenerationSaveError(trigger, times);
        init();
        testGenerationSaveNothingToSave(trigger, times);
    }

    @Test
    public void testListenerGenerationSave()
    throws IOException
    {
        testGenerationSave(()->this.listener.onGenerationSave(), 1);
    }

    private void testGenerationLoad
    (
        ArgumentCaptor<Runnable> captor,
        Runnable trigger,
        int times,
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

        trigger.run();
        verifyRunInBackground(captor);

        //show the file selection dialog
        verify(this.viewMock, times(times)).selectFile
        (
            eq(ViewBase.FileSelectionMode.SELECT_SINGLE),
            any(),
            any(),
            consumerCaptor.capture()
        );
        if (times == 0) return;

        //load if a file was selected
        List<File> files = new ArrayList<>();
        files.add(file);
        consumerCaptor.getValue().accept(files);
        verifyRunInBackground(captor, 2);
    }

    private void testGenerationLoadExistingFile(Runnable trigger, int times)
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Generation generation = mock(Generation.class);
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);

        testGenerationLoad(captor, trigger, times, file, generation);

        verify(this.modelMock, times(times)).setGeneration(generation);
    }

    private void testGenerationLoadNonExistingFile(Runnable trigger, int times)
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Generation generation = mock(Generation.class);
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);

        testGenerationLoad(captor, trigger, times, file, generation);

        verify(this.viewMock, times(times)).fireErrorAlert(eq("Generation loading failed"), any());
    }

    private void testGenerationLoadError(Runnable trigger, int times)
    throws IOException
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Generation generation = mock(Generation.class);
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        doThrow(new IOException()).when(this.fileIOMock).readAllBytes(any());

        testGenerationLoad(captor, trigger, times, file, generation);

        verify(this.viewMock, times(times)).fireErrorAlert(eq("Generation loading failed"), any());
    }

    private void testGenerationLoad(Runnable trigger, int times)
    throws IOException
    {
        testGenerationLoadExistingFile(trigger, times);
        init();
        testGenerationLoadNonExistingFile(trigger, times);
        init();
        testGenerationLoadError(trigger, times);
    }


    @Test
    public void testListenerGenerationLoad()
    throws IOException
    {
        testGenerationLoad( ()->this.listener.onGenerationLoad(), 1);
    }

    private void testHelp(Runnable trigger, int times)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);
        verify(this.viewMock, times(times)).fireInfoAlert
        (
            eq("Help"),
            and( startsWith(HELP_MSG_HEADER), endsWith(HELP_MSG_FOOTER) )
        );
    }

    @Test
    public void testHelp()
    {
        testHelp(()->this.listener.onHelp(), 1);
    }

    @Test
    public void testReadyForNextFrame()
    {
        Generation generation = mock(Generation.class);
        when(generation.getId()).thenReturn(10L);
        when(generation.getGenerationNumber()).thenReturn(2L);
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
