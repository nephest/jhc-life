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
import com.nephest.jhclife.io.*;

import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;

public class MainMenuViewImplFX
extends ViewBaseImplFX
implements MainMenuView<Parent>
{

    public static final String NEW_GAME_STRING = "New Game";
    public static final String CANCEL_STRING = "Cancel";

    private GridPane grid;
    private Spinner<Integer> widthSpinner, heightSpinner, seedSpinner;
    private Spinner<Double> probabilitySpinner;
    private Button newGameButton, cancelButton;

    public MainMenuViewImplFX(Window owner)
    {
        super(owner);
        init();
    }

    public MainMenuViewImplFX()
    {
        this(null);
    }

    @Override
    public Parent getRoot()
    {
        return this.grid;
    }

    // do not set the control accelerators directly, only show info about bindings
    @Override
    public void setControlBindingsInfo
    (ControlBindings<MainMenuPresenter.ControlType, ? extends Displayable>... binds)
    {
        setButtonBindingInfo
        (
            NEW_GAME_STRING, this.newGameButton, MainMenuPresenter.ControlType.NEW_GAME,
            binds
        );

        setButtonBindingInfo
        (
            CANCEL_STRING, this.cancelButton, MainMenuPresenter.ControlType.CANCEL,
            binds
        );
    }

    private void setButtonBindingInfo
    (
        String name,
        Button button,
        MainMenuPresenter.ControlType ctrl,
        ControlBindings<MainMenuPresenter.ControlType, ? extends Displayable>... binds
    )
    {
        String bindStr = ControlBindings.calculateControlName
        (
            name, FX.CONTROL_NAME_SPLITTER,
            FX.CONTROL_PREFIX, FX.CONTROL_SPLITTER, FX.CONTROL_SUFFIX,
            ctrl, binds
        );
        button.setText(bindStr);
    }

    @Override
    public void setListener(MainMenuViewListener listener)
    {
        if (listener == null)
        {
            unsetListener();
            return;
        }

        MainMenuView.Zone topZone = MainMenuView.Zone.GLOBAL;

        this.grid.setOnKeyPressed((e)->listener.onKeyEvent(e, topZone));
        this.grid.setOnKeyReleased((e)->listener.onKeyEvent(e, topZone));
        this.grid.setOnKeyTyped((e)->listener.onKeyEvent(e, topZone));

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
        this.grid.setOnKeyPressed(null);
        this.grid.setOnKeyReleased(null);
        this.grid.setOnKeyTyped(null);

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
        FX.standardSpinner(this.widthSpinner, 800, 1);

        this.heightSpinner = new Spinner(1, Integer.MAX_VALUE, 600, 1);
        this.heightSpinner.setEditable(true);
        FX.standardSpinner(this.heightSpinner, 600, 1);

        this.seedSpinner = new Spinner(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);
        this.seedSpinner.setEditable(true);
        FX.standardSpinner(this.seedSpinner, 0, 0);
        this.seedSpinner.setTooltip(new Tooltip("Provide the same seed and density to get the same population layout"));

        this.probabilitySpinner = new Spinner(0.0, 100.0, 50.0, 1.0);
        this.probabilitySpinner.setEditable(true);
        FX.standardSpinner(this.probabilitySpinner, 50.0, 0.0);
        this.probabilitySpinner.setTooltip(new Tooltip("Population density percentage"));

        this.newGameButton = new Button(NEW_GAME_STRING);
        this.newGameButton.setId("button-new-game");

        this.cancelButton = new Button(CANCEL_STRING);
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
