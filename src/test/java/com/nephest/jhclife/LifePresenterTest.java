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

import java.util.*;
import java.util.concurrent.*;

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

    private LifeView<?> viewMock;
    private ClassicLifeModel modelMock;
    private MainController controllerMock;
    private Executor executorMock;
    private LifeViewListener listener;

    private LifePresenter presenter;

    @Before
    public void init()
    {
        this.viewMock = mock(LifeView.class);
        this.modelMock = mock(ClassicLifeModel.class);
        this.controllerMock = mock(MainController.class);
        this.executorMock = mock(Executor.class);

        this.presenter = new LifePresenter
        (
            this.viewMock,
            this.modelMock,
            this.controllerMock,
            this.executorMock
        );

        this.listener = getListener();

        verify(this.viewMock).updateZoomInfo(ZOOM_FORMAT);
        verify(this.viewMock)
            .setSpeedInfo(String.format(SPEED_FORMAT, this.presenter.getSpeed()));
        //clear for easier zoom/speed testing
        //need to specify the exact invocation count or use ordering otherwise
        verifyNoMoreInteractions(this.viewMock);
        clearInvocations(this.viewMock);
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
        MouseEvent spy = spy(evt);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(spy, LifeView.Zone.GENERATION);
        verifyRunInBackground(captor);

        verify(this.modelMock).setPopulation( (int)x, (int)y, !alive);
        verify(spy, never()).consume();
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
        MouseEvent evt = new MouseEvent
        (
            MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, 1,
            true, false, false, false, //shift, ctrl, alt, meta
            false, false, false, //primary, mid, secondary
            false, false, false,
            null
        );
        MouseEvent spy = spy(evt);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(spy, zone);
        //consume in the GUI thread to properly stop the propagation chain
        if (zone != LifeView.Zone.GENERATION) verify(spy).consume();
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
        MouseEvent spy = spy(evt);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onMouseEvent(spy, zone);
        //consume in the GUI thread to properly stop the propagation chain
        if (zone != LifeView.Zone.GENERATION) verify(spy).consume();
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
        //the sign is the ONLY thing that matters
        double deltaY = down ? -32.0 : 1.0;
        ScrollEvent evt = new ScrollEvent
        (
            ScrollEvent.SCROLL, 0, 0, 0, 0, //type, x, y
            true, false, false, false, //shift, ctrl, alt, meta
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
        when(this.modelMock.isRunning()).thenReturn(running);

        KeyEvent evt = new KeyEvent
        (
            KeyEvent.KEY_RELEASED, "", "", code, //type, char, text, code
            false, false, false, false //shift, ctrl, alt, meta
        );

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onKeyEvent(evt, zone);

        assertFalse(evt.isConsumed());
        verifyRunInBackground(captor);

        if (zone != LifeView.Zone.GLOBAL)
        {
            verifyNoMoreInteractions(this.modelMock);
        }
        else if(running)
        {
            verify(this.modelMock).stop();
        }
        else
        {
            verify(this.modelMock).start();
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
    }

    @Test
    public void testPlay()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        this.listener.onPlay();
        verifyRunInBackground(captor);
        verify(this.modelMock).start();
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
