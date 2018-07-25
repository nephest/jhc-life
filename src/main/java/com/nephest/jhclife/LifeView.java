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

public interface LifeView<N>
extends ViewBase<N>
{

    public void setListener(LifeViewListener listener);

    public void render(Generation generation);

    public void reset();

    public void setGenerationZoom(double factor, int pivotX, int pivotY);

    public void setGenerationZoom(double factor);

    public double getFinalGenerationZoom();

    public void updateZoomInfo();

    public void setSpeedInfo(int speed);

}
