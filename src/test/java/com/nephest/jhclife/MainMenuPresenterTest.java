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

import java.util.concurrent.Executor;

import org.junit.*;
import static org.junit.Assert.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

public class MainMenuPresenterTest
{

    private MainMenuView<?> viewMock;
    private ClassicLifeModel modelMock;
    private MainController controllerMock;
    private Executor executorMock;

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
    }

    @Test
    public void testNewGame()
    {
        int width = 1;
        int height = 2;
        long seed = 3;
        double prob = 50.0;
        stubView(width, height, seed, prob);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        MainMenuViewListener listener = getListener();
        listener.onNewGame();
        verifyRunInBackground(captor);

        InOrder inOrder = inOrder(this.viewMock, this.modelMock, this.controllerMock);
        inOrder.verify(this.viewMock).lock();
        inOrder.verify(this.modelMock).stop();
        inOrder.verify(this.modelMock).createNewPopulation(width, height);
        inOrder.verify(this.modelMock).populate(seed, prob / 100.0);
        inOrder.verify(this.controllerMock).setViewType(MainView.ViewType.LIFE);
        inOrder.verify(this.viewMock).unlock();
    }

    @Test
    public void testNewGameInvalidWidth()
    {
        testNewGameInvalidParam
        (
            -1, 2, 3, 50,
            "Width and Height must be more than 0"
        );
    }

    @Test
    public void testNewGameInvalidHeight()
    {
        testNewGameInvalidParam
        (
            1, -2, 3, 50,
            "Width and Height must be more than 0"
        );
    }

    @Test
    public void testNewGameInvalidProbabilityLow()
    {
        testNewGameInvalidParam
        (
            1, 2, 3, -1.0,
            "Population density must be in 0-100 range"
        );
    }

    @Test
    public void testNewGameInvalidProbabilityHigh()
    {
        testNewGameInvalidParam
        (
            1, 2, 3, 100.1,
            "Population density must be in 0-100 range"
        );
    }

    private void testNewGameInvalidParam
    (
        int width,
        int height,
        long seed,
        double prob,
        String alert
    )
    {
        stubView(width, height, seed, prob);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        MainMenuViewListener listener = getListener();
        listener.onNewGame();
        verifyRunInBackground(captor);

        InOrder inOrder = inOrder(this.viewMock);
        inOrder.verify(this.viewMock).lock();
        inOrder.verify(this.viewMock).fireErrorAlert(alert);
        inOrder.verify(this.viewMock).unlock();

        verifyZeroInteractions(this.modelMock);
    }

    private void stubView(int width, int height, long seed, double prob)
    {
        when(this.viewMock.getWidth()).thenReturn(width);
        when(this.viewMock.getHeight()).thenReturn(height);
        when(this.viewMock.getSeed()).thenReturn(seed);
        when(this.viewMock.getPopulationProbability()).thenReturn(prob);
    }

    @Test
    public void testCancel()
    {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        MainMenuViewListener listener = getListener();
        listener.onCancel();
        verifyRunInBackground(captor);
        verify(this.controllerMock).setViewType(MainView.ViewType.LIFE);
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
