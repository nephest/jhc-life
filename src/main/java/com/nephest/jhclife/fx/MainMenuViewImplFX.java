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

package com.nephest.jhclife.fx;

import com.nephest.jhclife.*;

import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;

public class MainMenuViewImplFX
extends ViewBaseImplFX<GridPane>
implements MainMenuView<GridPane>
{
    private GridPane grid;
    private Spinner<Integer> widthSpinner, heightSpinner, seedSpinner;
    private Spinner<Double> probabilitySpinner;
    private Button newGameButton, cancelButton;

    public MainMenuViewImplFX()
    {
        super();
        init();
    }

    @Override
    public GridPane getRoot()
    {
        return this.grid;
    }

    @Override
    public void setListener(MainMenuViewListener listener)
    {
        if (listener == null)
        {
            unsetListener();
            return;
        }
        this.newGameButton.setOnAction((e)->listener.onNewGame());
        this.cancelButton.setOnAction((e)->listener.onCancel());
    }

    @Override
    public int getWidth()
    {
        return this.widthSpinner.getValue();
    }

    @Override
    public int getHeight()
    {
        return this.heightSpinner.getValue();
    }

    @Override
    public long getSeed()
    {
        return this.seedSpinner.getValue();
    }

    @Override
    public double getPopulationProbability()
    {
        return this.probabilitySpinner.getValue();
    }

    private void unsetListener()
    {
        this.newGameButton.setOnAction(null);
        this.cancelButton.setOnAction(null);
    }

    private void init()
    {
        initGrid();
        initControls();
        layoutControls();
    }

    private void initGrid()
    {
        this.grid = new GridPane();
        this.grid.setId("root-grid-main-menu");
    }

    private void initControls()
    {
        this.widthSpinner = new Spinner(1, Integer.MAX_VALUE, 800, 1);
        this.widthSpinner.setEditable(true);

        this.heightSpinner = new Spinner(1, Integer.MAX_VALUE, 600, 1);
        this.heightSpinner.setEditable(true);

        this.seedSpinner = new Spinner(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);
        this.seedSpinner.setEditable(true);
        this.seedSpinner.setTooltip(new Tooltip("Provide the same seed and density to get the same population layout"));

        this.probabilitySpinner = new Spinner(0.0, 100.0, 50.0, 1.0);
        this.probabilitySpinner.setEditable(true);
        this.probabilitySpinner.setTooltip(new Tooltip("Population density percentage"));

        this.newGameButton = new Button("New Game");
        this.newGameButton.setId("button-new-game");

        this.cancelButton = new Button("Cancel");
        this.cancelButton.setId("button-cancel");
    }

    private void layoutControls()
    {
        this.grid.add(new Label("Width"), 0, 0);
        this.grid.add(this.widthSpinner, 1, 0);

        this.grid.add(new Label("Height"), 2, 0);
        this.grid.add(this.heightSpinner, 3, 0);

        this.grid.add(new Label("Seed"), 0, 1);
        this.grid.add(this.seedSpinner, 1, 1);

        this.grid.add(new Label("Density"), 2, 1);
        this.grid.add(this.probabilitySpinner, 3, 1);

        this.grid.add(this.newGameButton, 0, 2, 2, 1);
        this.grid.add(this.cancelButton, 2, 2, 2, 1);
    }

}
