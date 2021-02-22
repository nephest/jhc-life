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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.*;

public class MainMenuPresenterTest
{

    public static final KeyCode CANCEL = LifePresenterTest.NEW_GAME;

    private MainMenuView<?> viewMock;
    private ClassicLifeModel modelMock;
    private MainController controllerMock;
    private Executor executorMock;
    private MainMenuViewListener listener;

    private MainMenuPresenter presenter;

    @Before
    public void init()
    {
        this.viewMock = mock(MainMenuView.class);
        this.modelMock = mock(ClassicLifeModel.class);
        this.controllerMock = mock(MainController.class);
        this.executorMock = mock(Executor.class);

        this.presenter = new MainMenuPresenter
        (
            this.viewMock,
            this.modelMock,
            this.controllerMock,
            this.executorMock
        );

        this.listener = getListener();
    }

    @Test
    public void testKeyCancel()
    {
        for (MainMenuView.Zone zone : MainMenuView.Zone.values())
        {
            init();
            KeyEvent evt = new KeyEvent
            (
                KeyEvent.KEY_PRESSED, "", "", CANCEL, //type, char, text, code
                false, false, false, false //shift, ctrl, alt, meta
            );
            Runnable trigger = ()->this.listener.onKeyEvent(evt, zone);
            if (zone == MainMenuView.Zone.GLOBAL)
            {
                testCancel(trigger, 1);
            }
            else
            {
                testCancel(trigger, 0);
            }
        }
    }

    private void testNewGame(Runnable trigger, int times)
    {
        int width = 1;
        int height = 2;
        long seed = 3;
        double prob = 50.0;
        stubView(width, height, seed, prob);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);

        InOrder inOrder = inOrder(this.viewMock, this.modelMock, this.controllerMock);
        inOrder.verify(this.viewMock, times(times)).lock();
        inOrder.verify(this.modelMock, times(times)).stop();
        inOrder.verify(this.modelMock, times(times)).createNewPopulation(width, height);
        inOrder.verify(this.modelMock, times(times)).populate(seed, prob / 100.0);
        inOrder.verify(this.controllerMock, times(times)).setViewType(MainView.ViewType.LIFE);
        inOrder.verify(this.viewMock, times(times)).unlock();
    }

    private void testNewGameInvalidWidth(Runnable trigger, int times)
    {
        testNewGameInvalidParam
        (
            trigger, times,
            -1, 2, 3, 50,
            "Invalid dimensions",
            "Width and Height must be more than 0"
        );
    }

    private void testNewGameInvalidHeight(Runnable trigger, int times)
    {
        testNewGameInvalidParam
        (
            trigger, times,
            1, -2, 3, 50,
            "Invalid dimensions",
            "Width and Height must be more than 0"
        );
    }

    private void testNewGameInvalidProbabilityLow(Runnable trigger, int times)
    {
        testNewGameInvalidParam
        (
            trigger, times,
            1, 2, 3, -1.0,
            "Invalid density",
            "Population density must be in 0-100 range"
        );
    }

    private void testNewGameInvalidProbabilityHigh(Runnable trigger, int times)
    {
        testNewGameInvalidParam
        (
            trigger, times,
            1, 2, 3, 100.1,
            "Invalid density",
            "Population density must be in 0-100 range"
        );
    }

    private void testNewGameInvalidParam
    (
        Runnable trigger,
        int times,
        int width,
        int height,
        long seed,
        double prob,
        String header,
        String alert
    )
    {
        stubView(width, height, seed, prob);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);

        InOrder inOrder = inOrder(this.viewMock);
        inOrder.verify(this.viewMock, times(times)).lock();
        inOrder.verify(this.viewMock, times(times)).fireErrorAlert(header, alert);
        inOrder.verify(this.viewMock, times(times)).unlock();

        verifyZeroInteractions(this.modelMock);
    }

    private void stubView(int width, int height, long seed, double prob)
    {
        when(this.viewMock.getWidth()).thenReturn(width);
        when(this.viewMock.getHeight()).thenReturn(height);
        when(this.viewMock.getSeed()).thenReturn(seed);
        when(this.viewMock.getPopulationProbability()).thenReturn(prob);
    }

    private void testNewGameFull(Runnable trigger, int times)
    {
        testNewGame(trigger, times);
        init();
        testNewGameInvalidWidth(trigger, times);
        init();
        testNewGameInvalidHeight(trigger, times);
        init();
        testNewGameInvalidProbabilityLow(trigger, times);
        init();
        testNewGameInvalidProbabilityHigh(trigger, times);
    }

    @Test
    public void testListenerNewGame()
    {
        testNewGameFull( ()->this.listener.onNewGame(), 1 );
    }

    private void testCancel(Runnable trigger, int times)
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        trigger.run();
        verifyRunInBackground(captor);
        verify(this.controllerMock, times(times)).setViewType(MainView.ViewType.LIFE);
    }

    @Test
    public void testListenerCancel()
    {
        testCancel( ()->this.listener.onCancel(), 1 );
    }

    private MainMenuViewListener getListener()
    {
        ArgumentCaptor<MainMenuViewListener> captor
            = ArgumentCaptor.forClass(MainMenuViewListener.class);
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
