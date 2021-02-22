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

package com.nephest.jhclife.fx;

import com.nephest.jhclife.MainMenuPresenter;
import com.nephest.jhclife.MainMenuView;
import com.nephest.jhclife.MainMenuViewListener;
import com.nephest.jhclife.io.ControlBindings;
import com.nephest.jhclife.io.Displayable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

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
    @SafeVarargs
    @Override
    public final void setControlBindingsInfo(ControlBindings<MainMenuPresenter.ControlType, ? extends Displayable>... binds)
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

    @SafeVarargs
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
        this.widthSpinner = new Spinner<>(1, Integer.MAX_VALUE, 800, 1);
        this.widthSpinner.setEditable(true);
        FX.standardSpinner(this.widthSpinner, 800, 1);

        this.heightSpinner = new Spinner<>(1, Integer.MAX_VALUE, 600, 1);
        this.heightSpinner.setEditable(true);
        FX.standardSpinner(this.heightSpinner, 600, 1);

        this.seedSpinner = new Spinner<>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);
        this.seedSpinner.setEditable(true);
        FX.standardSpinner(this.seedSpinner, 0, 0);
        this.seedSpinner.setTooltip(new Tooltip("Provide the same seed and density to get the same population layout"));

        this.probabilitySpinner = new Spinner<>(0.0, 100.0, 50.0, 1.0);
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
