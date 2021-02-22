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

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public interface LifeViewListener
{

    public void onMouseEvent(MouseEvent evt, LifeView.Zone zone);

    public void onScrollEvent(ScrollEvent evt, LifeView.Zone zone);

    public void onKeyEvent(KeyEvent evt, LifeView.Zone zone);

    public void onZoomUp();

    public void onZoomDown();

    public void onZoomDefault();

    public void onSpeedUp();

    public void onSpeedDown();

    public void onSpeedDefault();

    public void onStateToggle();

    public void onNewGame();

    public void onGenerationSave();

    public void onGenerationLoad();

    public void onHelp();

    public void readyForNextFrame();

}

