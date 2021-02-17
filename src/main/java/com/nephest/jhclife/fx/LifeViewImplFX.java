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

import java.util.*;
import java.util.concurrent.*;

import javafx.application.Platform;
import javafx.animation.*;
import javafx.beans.value.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.image.*;
import javafx.geometry.*;

public class LifeViewImplFX
extends ViewBaseImplFX
implements LifeView<Parent>
{

    public static final Color ALIVE_COLOR = Color.BLACK;
    public static final Color DEAD_COLOR = Color.WHITE;

    public static final String BUTTON_PLUS_CLASS="button-plus";
    public static final String BUTTON_MINUS_CLASS="button-minus";
    public static final String BUTTON_DEFAULT_CLASS="button-default";
    public static final String GROUP_BUTTON_CLASS="group-button";
    public static final String SEPARATOR_CLASS="separator";
    public static final String LABEL_LABEL_CLASS="label-label";
    public static final String LABEL_VALUE_CLASS="label-value";
    public static final String SPACER_CLASS="spacer";
    public static final String MENU_ITEM_CLASS="menu-item";

    public static final String NEW_GAME_STRING = "New";
    public static final String GENERATION_SAVE_STRING = "Save";
    public static final String GENERATION_LOAD_STRING = "Load";
    public static final String HELP_STRING = "Help";
    public static final String STATE_TOGGLE_STRING = "Play/Pause";

    private Generation lastGeneration;

    private BorderPane borderPane;
    private Button stateToggleButton,
        speedUpButton, speedDownButton, speedDefaultButton,
        zoomUpButton, zoomDownButton, zoomDefaultButton;
    private Label generationNumberLabel, tipLabel, statusLabel, speedLabel, zoomLabel;
    private MenuItem newGameItem, generationSaveItem, generationLoadItem, helpItem;

    private LifeViewListener listener;
    private AnimationTimer frameTimer;

    private ScrollPane generationScroll;
    private StackPane generationPane;
    private Group generationGroup;
    private WritableImage generationImage;
    private WritableImage generationImage2;
    private WritableImage currentGenerationImage = generationImage;
    private ImageView generationImageView;

    private Map<WritableImage, Generation> generationImageMap = new HashMap();

    public LifeViewImplFX(Window owner)
    {
        super(owner);
        init();
    }

    public LifeViewImplFX()
    {
        this(null);
    }

    @Override
    public Parent getRoot()
    {
        return this.borderPane;
    }

    // do not set the control accelerators directly, only show info about bindings
    @Override
    public void setControlBindingsInfo
    (ControlBindings<LifePresenter.ControlType, ? extends Displayable>... binds)
    {
        String menuNameSplitter = "\t\t";

        setMenuItemBindingInfo
        (
            NEW_GAME_STRING, menuNameSplitter, this.newGameItem,
            LifePresenter.ControlType.NEW_GAME, binds
        );

        setMenuItemBindingInfo
        (
            GENERATION_SAVE_STRING, menuNameSplitter, this.generationSaveItem,
            LifePresenter.ControlType.GENERATION_SAVE, binds
        );

        setMenuItemBindingInfo
        (
            GENERATION_LOAD_STRING, menuNameSplitter, this.generationLoadItem,
            LifePresenter.ControlType.GENERATION_LOAD, binds
        );

        setMenuItemBindingInfo
        (
            HELP_STRING, menuNameSplitter, this.helpItem,
            LifePresenter.ControlType.HELP, binds
        );

        setButtonBindingInfo
        (
            STATE_TOGGLE_STRING, this.stateToggleButton, LifePresenter.ControlType.STATE_TOGGLE,
            binds
        );
    }

    private void setMenuItemBindingInfo
    (
        String name,
        String splitter,
        MenuItem item,
        LifePresenter.ControlType ctrl,
        ControlBindings<LifePresenter.ControlType, ? extends Displayable>... binds
    )
    {
        String bindStr = ControlBindings.calculateControlName
        (
            name, splitter, FX.CONTROL_SPLITTER,
            ctrl, binds
        );
        item.setText(bindStr);
    }

    private void setButtonBindingInfo
    (
        String name,
        Button button,
        LifePresenter.ControlType ctrl,
        ControlBindings<LifePresenter.ControlType, ? extends Displayable>... binds
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
    public void setListener(LifeViewListener listener)
    {
        unsetListener();
        this.listener = listener;
        if (listener == null) return;

        LifeView.Zone generationZone = LifeView.Zone.GENERATION;
        LifeView.Zone generationPaneZone = LifeView.Zone.GENERATION_CONTAINER;
        LifeView.Zone topZone = LifeView.Zone.GLOBAL;

        this.generationImageView.setOnMouseClicked((e)->listener.onMouseEvent(e, generationZone));
        this.generationImageView.setOnScroll((e)->listener.onScrollEvent(e, generationZone));
        this.generationImageView.setOnKeyPressed((e)->listener.onKeyEvent(e, generationZone));
        this.generationImageView.setOnKeyReleased((e)->listener.onKeyEvent(e, generationZone));
        this.generationImageView.setOnKeyTyped((e)->listener.onKeyEvent(e, generationZone));

        this.generationPane.setOnMouseClicked((e)->listener.onMouseEvent(e, generationPaneZone));
        this.generationPane.setOnScroll((e)->listener.onScrollEvent(e, generationPaneZone));
        this.generationPane.setOnKeyPressed((e)->listener.onKeyEvent(e, generationPaneZone));
        this.generationPane.setOnKeyReleased((e)->listener.onKeyEvent(e, generationPaneZone));
        this.generationPane.setOnKeyTyped((e)->listener.onKeyEvent(e, generationPaneZone));

        this.borderPane.setOnMouseClicked((e)->listener.onMouseEvent(e, topZone));
        this.borderPane.setOnScroll((e)->listener.onScrollEvent(e, topZone));
        this.borderPane.setOnKeyPressed((e)->listener.onKeyEvent(e, topZone));
        this.borderPane.setOnKeyReleased((e)->listener.onKeyEvent(e, topZone));
        this.borderPane.setOnKeyTyped((e)->listener.onKeyEvent(e, topZone));

        this.zoomUpButton.setOnAction((e)->listener.onZoomUp());
        this.zoomDownButton.setOnAction((e)->listener.onZoomDown());
        this.zoomDefaultButton.setOnAction((e)->listener.onZoomDefault());

        this.speedUpButton.setOnAction((e)->listener.onSpeedUp());
        this.speedDownButton.setOnAction((e)->listener.onSpeedDown());
        this.speedDefaultButton.setOnAction((e)->listener.onSpeedDefault());

        this.stateToggleButton.setOnAction((e)->listener.onStateToggle());

        this.newGameItem.setOnAction((e)->listener.onNewGame());
        this.generationSaveItem.setOnAction((e)->listener.onGenerationSave());
        this.generationLoadItem.setOnAction((e)->listener.onGenerationLoad());
        this.helpItem.setOnAction((e)->listener.onHelp());

        getFrameTimer().start();
    }

    //calling thread must be FX thread
    @Override
    public void render(Generation generation)
    {
        Objects.requireNonNull(generation);
        prepareGenerationView(generation);
        nextGenerationImage();
        backgroundRender(generation);
    }

    private void backgroundRender(Generation generation)
    {
        if (isFirstGeneration(generation))
        {
            renderFirstGeneraiton(generation);
        }
        else
        {
            renderGenerationDelta(this.generationImageMap.get(this.currentGenerationImage), generation);
        }
        this.lastGeneration = generation;
        this.generationImageMap.put(this.currentGenerationImage, generation);

        this.generationImageView.setImage(currentGenerationImage);
        this.generationNumberLabel
            .setText(String.valueOf(generation.getGenerationNumber()));

    }

    private boolean isFirstGeneration(Generation generation)
    {
        return this.generationImageMap.get(generationImage) == null
            || this.generationImageMap.get(generationImage2) == null
            || this.generationImageMap.get(generationImage).getId()
                != this.generationImageMap.get(generationImage2).getId()
            || getLastGeneration().getWidth() != generation.getWidth()
            || getLastGeneration().getHeight() != generation.getHeight();
    }

    private void renderFirstGeneraiton(Generation generation)
    {
        PixelWriter pixelWriter = this.currentGenerationImage.getPixelWriter();
        for (int col = 0; col < generation.getWidth(); col++)
        {
            for (int row = 0; row < generation.getHeight(); row++)
            {
                if (generation.isPopulationAlive(col, row))
                {
                    pixelWriter.setColor(col, row, ALIVE_COLOR);
                }
                else
                {
                    pixelWriter.setColor(col, row, DEAD_COLOR);
                }
            }
        }
    }

    private void renderGenerationDelta(Generation prev, Generation next)
    {
        PixelWriter pixelWriter = this.currentGenerationImage.getPixelWriter();
        ForkJoinPool.commonPool()
            .invoke( new MatrixPixelWriter(prev, next, pixelWriter));
    }

    @Override
    public void reset()
    {
        Platform.runLater( ()->doReset() );
    }

    private void doReset()
    {
        PixelWriter pixelWriter = this.generationImage.getPixelWriter();
        for (int col = 0; col < this.generationImage.getWidth(); col++)
        {
            for (int row = 0; row < this.generationImage.getHeight(); row++)
            {
                pixelWriter.setColor(col, row, DEAD_COLOR);
            }
        }

        PixelWriter pixelWriter2 = this.generationImage2.getPixelWriter();
        for (int col = 0; col < this.generationImage2.getWidth(); col++)
        {
            for (int row = 0; row < this.generationImage2.getHeight(); row++)
            {
                pixelWriter2.setColor(col, row, DEAD_COLOR);
            }
        }

        this.generationImageMap.clear();
        this.generationNumberLabel.setText("0");
    }

    @Override
    public void setGenerationZoom(double factor, int pivotX, int pivotY)
    {
        Platform.runLater( ()->doSetGenerationZoom(factor, pivotX, pivotY) );
    }

    private void doSetGenerationZoom(double factor, int pivotX, int pivotY)
    {
        scaleGenerationView(factor, pivotX, pivotY);
    }

    @Override
    public void setGenerationZoom(double factor)
    {
        Platform.runLater( ()->doSetGenerationZoom(factor) );
    }

    private void doSetGenerationZoom(double factor)
    {
        Bounds bounds = this.generationScroll.getViewportBounds();
        int pivotX = (int) (Math.abs(bounds.getMinX()) + bounds.getWidth() / 2);
        int pivotY = (int) (Math.abs(bounds.getMinY()) + bounds.getHeight() / 2);
        scaleGenerationView(factor, pivotX, pivotY);
    }

    @Override
    public double getFinalGenerationZoom()
    {
        return this.generationImageView.getScaleX();
    }

    @Override
    public void updateZoomInfo(String format)
    {
        Platform.runLater
        (
            ()->this.zoomLabel.setText(String.format(format, getFinalGenerationZoom()))
        );
    }

    @Override
    public void setSpeedInfo(String speed)
    {
        Platform.runLater( ()->this.speedLabel.setText(speed) );
    }

    @Override
    public void setStatus(String status)
    {
        Platform.runLater( ()->this.statusLabel.setText(status) );
    }

    @Override
    public void setTip(String tip)
    {
        Platform.runLater( ()->this.tipLabel.setText(tip) );
    }

    private void unsetListener()
    {
        this.generationImageView.setOnMouseClicked(null);
        this.generationImageView.setOnScroll(null);
        this.generationImageView.setOnKeyPressed(null);
        this.generationImageView.setOnKeyReleased(null);
        this.generationImageView.setOnKeyTyped(null);

        this.generationPane.setOnMouseClicked(null);
        this.generationPane.setOnScroll(null);
        this.generationPane.setOnKeyPressed(null);
        this.generationPane.setOnKeyReleased(null);
        this.generationPane.setOnKeyTyped(null);

        this.borderPane.setOnMouseClicked(null);
        this.borderPane.setOnScroll(null);
        this.borderPane.setOnKeyPressed(null);
        this.borderPane.setOnKeyReleased(null);
        this.borderPane.setOnKeyTyped(null);

        this.zoomUpButton.setOnAction(null);
        this.zoomDownButton.setOnAction(null);
        this.zoomDefaultButton.setOnAction(null);

        this.speedUpButton.setOnAction(null);
        this.speedDownButton.setOnAction(null);
        this.speedDefaultButton.setOnAction(null);

        this.stateToggleButton.setOnAction(null);

        this.newGameItem.setOnAction(null);
        this.generationSaveItem.setOnAction(null);
        this.generationLoadItem.setOnAction(null);
        this.helpItem.setOnAction(null);

        getFrameTimer().stop();
    }

    private void prepareGenerationView(Generation generation)
    {
        if
        (
            this.generationImage == null
            || this.generationImage.getWidth() != generation.getWidth()
            || this.generationImage.getHeight() != generation.getHeight()
        )
        {
            this.generationImage = new WritableImage(generation.getWidth(), generation.getHeight());
            this.generationImage2 = new WritableImage(generation.getWidth(), generation.getHeight());
            this.generationImageMap.clear();
        }
    }

    private void init()
    {
        initBorderPane();
        initControls();
        layoutControls();
        initGenerationView();
        initFrameTimer();
    }

    private void initBorderPane()
    {
        this.borderPane = new BorderPane();
        this.borderPane.setId("root-life");
    }

    private void initControls()
    {

        this.speedLabel = newValueLabel(false);
        this.speedLabel.setId("label-speed");

        this.speedUpButton = new Button("+");
        this.speedUpButton.getStyleClass().add(BUTTON_PLUS_CLASS);
        this.speedDownButton = new Button("-");
        this.speedDownButton.getStyleClass().add(BUTTON_MINUS_CLASS);
        this.speedDefaultButton = new Button("default");
        this.speedDefaultButton.getStyleClass().add(BUTTON_DEFAULT_CLASS);

        this.zoomLabel = newValueLabel(false);
        this.zoomLabel.setId("label-zoom");

        this.zoomUpButton = new Button("+");
        this.zoomUpButton.getStyleClass().add(BUTTON_PLUS_CLASS);
        this.zoomDownButton = new Button("-");
        this.zoomDownButton.getStyleClass().add(BUTTON_MINUS_CLASS);
        this.zoomDefaultButton = new Button("default");
        this.zoomDefaultButton.getStyleClass().add(BUTTON_DEFAULT_CLASS);

        this.stateToggleButton = new Button(STATE_TOGGLE_STRING);
        this.stateToggleButton.setId("button-state-toggle");

        this.newGameItem = new MenuItem(NEW_GAME_STRING);
        this.newGameItem.getStyleClass().add(MENU_ITEM_CLASS);

        this.generationSaveItem = new MenuItem(GENERATION_SAVE_STRING);
        this.generationSaveItem.getStyleClass().add(MENU_ITEM_CLASS);
        this.generationLoadItem = new MenuItem(GENERATION_LOAD_STRING);
        this.generationLoadItem.getStyleClass().add(MENU_ITEM_CLASS);
        this.helpItem = new MenuItem(HELP_STRING);
        this.helpItem.getStyleClass().add(MENU_ITEM_CLASS);

        this.generationNumberLabel = newValueLabel(false);
        this.generationNumberLabel.setId("label-generation-number");

        this.tipLabel = newValueLabel(true);
        this.tipLabel.setId("label-tip");

        this.statusLabel = newValueLabel(false);
        this.statusLabel.setId("label-status");
    }

    private void layoutControls()
    {
        MenuBar mainMenuBar = new MenuBar
        (
            newMenu("Game", newGameItem, generationLoadItem, generationSaveItem),
            newMenu("Help", helpItem)
        );
        mainMenuBar.setId("menu-main");

        HBox ctrls = new HBox
        (
            newLabel("Speed"),
            this.speedDownButton, this.speedLabel,
            newButtonGroup(this.speedUpButton, this.speedDefaultButton),
            newSeparator(Orientation.VERTICAL),

            newLabel("Zoom"),
            this.zoomDownButton, this.zoomLabel,
            newButtonGroup(this.zoomUpButton, this.zoomDefaultButton),
            newSeparator(Orientation.VERTICAL),

            newButtonGroup(this.stateToggleButton),
            newSpacer()
        );
        ctrls.setId("box-control");
        HBox info = new HBox
        (
            newLabel("Generation:"), this.generationNumberLabel,
            newSeparator(Orientation.VERTICAL),
            newLabel("Status:"), this.statusLabel,
            newSeparator(Orientation.VERTICAL),
            newLabel("Tip:"), this.tipLabel
        );
        info.setId("box-info");
        VBox allCtrls = new VBox(mainMenuBar, ctrls, info);
        allCtrls.setId("box-all");
        this.borderPane.setTop(allCtrls);
    }

    private Menu newMenu(String name, MenuItem... items)
    {
        Menu menu = new Menu(name);
        menu.getItems().addAll(items);
        return menu;
    }

    private Region newSpacer()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.getStyleClass().add(SPACER_CLASS);
        return spacer;
    }

    private Separator newSeparator(Orientation orientation)
    {
        Separator result = new Separator(orientation);
        result.getStyleClass().add(SEPARATOR_CLASS);
        return result;
    }

    private Label newLabel(String content)
    {
        Label result = new Label(content);
        result.getStyleClass().add(LABEL_LABEL_CLASS);
        return result;
    }

    private Label newValueLabel(boolean ellipsis)
    {
        Label result = new Label();
        result.getStyleClass().add(LABEL_VALUE_CLASS);
        if (!ellipsis) result.setMinWidth(Label.USE_PREF_SIZE);
        return result;
    }

    private HBox newButtonGroup(Button... buttons)
    {
        HBox box = new HBox(buttons);
        box.getStyleClass().add(GROUP_BUTTON_CLASS);
        return box;
    }

    private void initGenerationView()
    {
        this.generationImageView = new ImageView();
        this.generationImageView.setId("population");
        this.generationGroup = new Group(this.generationImageView);
        this.generationPane = new StackPane(this.generationGroup);
        this.generationGroup.layoutBoundsProperty().addListener
        (
            (o, ov, nv)->
            {
                this.generationPane.setMinWidth(nv.getWidth());
                this.generationPane.setMinHeight(nv.getHeight());
            }
        );

        this.generationScroll = new ScrollPane(this.generationPane);
        this.generationScroll.setPannable(true);
        this.generationScroll.viewportBoundsProperty().addListener
        (
            (o, ov, nv)->
            {
                this.generationPane.setPrefWidth(nv.getWidth());
                this.generationPane.setPrefHeight(nv.getHeight());
            }
        );

        this.borderPane.setCenter(this.generationScroll);
    }

    private void initFrameTimer()
    {
        this.frameTimer = new AnimationTimer()
        {
            @Override
            public void handle(long pulses)
            {
                if (getListener() != null) getListener().readyForNextFrame();
            }
        };
    }

    private WritableImage nextGenerationImage()
    {
        WritableImage next =
            this.generationImage == this.currentGenerationImage
            ? this.generationImage2
            : this.generationImage;
        this.currentGenerationImage = next;
        return this.currentGenerationImage;
    }

    private Generation getLastGeneration()
    {
        return this.lastGeneration;
    }

    private LifeViewListener getListener()
    {
        return this.listener;
    }

    private AnimationTimer getFrameTimer()
    {
        return this.frameTimer;
    }

    private void scaleGenerationView(double factor, int pivotX, int pivotY)
    {
        if (factor == 0.0)
        {
            this.generationImageView.setScaleX(1);
            this.generationImageView.setScaleY(1);
            return;
        }

        Bounds groupBounds = this.generationGroup.getLayoutBounds();
        Bounds viewportBounds = generationScroll.getViewportBounds();

        double valX = this.generationScroll.getHvalue()
            * (groupBounds.getWidth() - viewportBounds.getWidth());
        double valY = this.generationScroll.getVvalue()
            * (groupBounds.getHeight() - viewportBounds.getHeight());

        Point2D posInZoomTarget = this.generationImageView
            .parentToLocal(this.generationGroup.parentToLocal(new Point2D(pivotX, pivotY)));

        Point2D adjustment = this.generationImageView.getLocalToParentTransform()
            .deltaTransform(posInZoomTarget.multiply(factor - 1));

        this.generationImageView.setScaleX(factor * this.generationImageView.getScaleX());
        this.generationImageView.setScaleY(factor * this.generationImageView.getScaleY());

        this.generationScroll.layout();

        groupBounds = this.generationGroup.getLayoutBounds();
        this.generationScroll.setHvalue
        (
            (valX + adjustment.getX())
            / (groupBounds.getWidth() - viewportBounds.getWidth())
        );
        this.generationScroll.setVvalue
        (
            (valY + adjustment.getY())
            / (groupBounds.getHeight() - viewportBounds.getHeight())
        );
    }

}
