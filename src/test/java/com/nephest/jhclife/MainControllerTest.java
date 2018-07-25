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

import org.junit.*;
import static org.junit.Assert.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

public class MainControllerTest
{

    private MainController controller;
    private MainView mainViewMock;

    @Before
    public void init()
    {
        this.mainViewMock = mock(MainView.class);
        this.controller = new MainController(this.mainViewMock);
    }

    @Test
    public void testSetViewType()
    {
        for (MainView.ViewType type : MainView.ViewType.values())
        {
            controller.setViewType(type);
            verify(mainViewMock).setViewType(type);
        }
    }

}
