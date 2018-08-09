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

import java.util.*;

import javafx.application.Platform;
import javafx.animation.*;
import javafx.beans.value.*;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.image.*;
import javafx.geometry.*;

public class LifeViewImplFX
extends ViewBaseImplFX<Parent>
implements LifeView<Parent>
{

    public static final Color ALIVE_COLOR = Color.BLACK;
    public static final Color DEAD_COLOR = Color.WHITE;

    public static final String BUTTON_PLUS_CLASS="button-plus";
    public static final String BUTTON_MINUS_CLASS="button-minus";
    public static final String BUTTON_DEFAULT_CLASS="button-default";
    public static final String GROUP_BUTTON_CLASS="group-button";
    public static final String SEPARATOR_CLASS="separator";
    public static final String TEXT_LABEL_CLASS="text-label";
    public static final String TEXT_VALUE_CLASS="text-value";

    private Generation lastGeneration;

    private BorderPane borderPane;
    private Button playButton, pauseButton, newGameButton,
        speedUpButton, speedDownButton, speedDefaultButton,
        zoomUpButton, zoomDownButton, zoomDefaultButton;
    private Text generationNumberText, tipText, statusText, speedText, zoomText;

    private LifeViewListener listener;
    private AnimationTimer frameTimer;

    private ScrollPane canvasScroll;
    private StackPane canvasPane;
    private Group canvasGroup;
    private Canvas canvas;

    LifeViewImplFX()
    {
        super();
        init();
    }

    @Override
    public Parent getRoot()
    {
        return this.borderPane;
    }

    @Override
    public void setListener(LifeViewListener listener)
    {
        unsetListener();
        this.listener = listener;
        if (listener == null) return;

        LifeView.Zone canvasZone = LifeView.Zone.GENERATION;
        LifeView.Zone canvasPaneZone = LifeView.Zone.GENERATION_CONTAINER;
        LifeView.Zone topZone = LifeView.Zone.GLOBAL;
        this.canvas.setOnMouseClicked((e)->listener.onMouseEvent(e, canvasZone));
        this.canvas.setOnScroll((e)->listener.onScrollEvent(e, canvasZone));
        this.canvasPane.setOnMouseClicked((e)->listener.onMouseEvent(e, canvasPaneZone));
        this.canvasPane.setOnScroll((e)->listener.onScrollEvent(e, canvasPaneZone));
        this.borderPane.setOnMouseClicked((e)->listener.onMouseEvent(e, topZone));
        this.borderPane.setOnScroll((e)->listener.onScrollEvent(e, topZone));

        this.zoomUpButton.setOnAction((e)->listener.onZoomUp());
        this.zoomDownButton.setOnAction((e)->listener.onZoomDown());
        this.zoomDefaultButton.setOnAction((e)->listener.onZoomDefault());

        this.speedUpButton.setOnAction((e)->listener.onSpeedUp());
        this.speedDownButton.setOnAction((e)->listener.onSpeedDown());
        this.speedDefaultButton.setOnAction((e)->listener.onSpeedDefault());

        this.playButton.setOnAction((e)->listener.onPlay());
        this.pauseButton.setOnAction((e)->listener.onPause());
        this.newGameButton.setOnAction((e)->listener.onNewGame());
        getFrameTimer().start();
    }

    //calling thread must be FX thread
    @Override
    public void render(Generation generation)
    {
        Objects.requireNonNull(generation);
        prepareCanvas(generation);
        if (isFirstGeneration(generation))
        {
            renderFirstGeneraiton(generation);
        }
        else
        {
            renderGenerationDelta(getLastGeneration(), generation);
        }
        this.generationNumberText
            .setText(String.valueOf(generation.getGenerationNumber()));
        this.lastGeneration = generation;
    }

    private boolean isFirstGeneration(Generation generation)
    {
        return getLastGeneration() == null
            || getLastGeneration().getWidth() != generation.getWidth()
            || getLastGeneration().getHeight() != generation.getHeight();
    }

    private void renderFirstGeneraiton(Generation generation)
    {
        this.canvas.getGraphicsContext2D()
            .fillRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
        PixelWriter pixelWriter = this.canvas.getGraphicsContext2D().getPixelWriter();
        for (int col = 0; col < generation.getWidth(); col++)
        {
            for (int row = 0; row < generation.getHeight(); row++)
            {
                if (generation.isPopulationAlive(col, row))
                    pixelWriter.setColor(col, row, ALIVE_COLOR);
            }
        }
    }

    private void renderGenerationDelta(Generation prev, Generation next)
    {
        PixelWriter pixelWriter = this.canvas.getGraphicsContext2D().getPixelWriter();
        for (int col = 0; col < next.getWidth(); col++)
        {
            for (int row = 0; row < next.getHeight(); row++)
            {
                Color nColor = next.isPopulationAlive(col, row)
                    ? ALIVE_COLOR
                    : DEAD_COLOR;
                Color pColor = prev.isPopulationAlive(col, row)
                    ? ALIVE_COLOR
                    : DEAD_COLOR;
                if (nColor != pColor) pixelWriter.setColor(col, row, nColor);
            }
        }
    }

    @Override
    public void reset()
    {
        Platform.runLater( ()->doReset() );
    }

    private void doReset()
    {
        GraphicsContext context = this.canvas.getGraphicsContext2D();
        context.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
        this.generationNumberText.setText("0");
    }

    @Override
    public void setGenerationZoom(double factor, int pivotX, int pivotY)
    {
        Platform.runLater( ()->doSetGenerationZoom(factor, pivotX, pivotY) );
    }

    private void doSetGenerationZoom(double factor, int pivotX, int pivotY)
    {
        scaleCanvas(factor, pivotX, pivotY);
    }

    @Override
    public void setGenerationZoom(double factor)
    {
        Platform.runLater( ()->doSetGenerationZoom(factor) );
    }

    private void doSetGenerationZoom(double factor)
    {
        Bounds bounds = this.canvasScroll.getViewportBounds();
        int pivotX = (int) (Math.abs(bounds.getMinX()) + bounds.getWidth() / 2);
        int pivotY = (int) (Math.abs(bounds.getMinY()) + bounds.getHeight() / 2);
        scaleCanvas(factor, pivotX, pivotY);
    }

    @Override
    public double getFinalGenerationZoom()
    {
        return this.canvas.getScaleX();
    }

    @Override
    public void updateZoomInfo()
    {
        Platform.runLater
        (
            ()->this.zoomText.setText(String.valueOf(getFinalGenerationZoom()))
        );
    }

    @Override
    public void setSpeedInfo(int speed)
    {
        Platform.runLater( ()->this.speedText.setText(String.valueOf(speed)) );
    }

    private void unsetListener()
    {
        this.canvas.setOnMouseClicked(null);
        this.canvas.setOnScroll(null);
        this.canvasPane.setOnMouseClicked(null);
        this.canvasPane.setOnScroll(null);
        this.borderPane.setOnMouseClicked(null);
        this.borderPane.setOnScroll(null);

        this.zoomUpButton.setOnAction(null);
        this.zoomDownButton.setOnAction(null);
        this.zoomDefaultButton.setOnAction(null);

        this.speedUpButton.setOnAction(null);
        this.speedDownButton.setOnAction(null);
        this.speedDefaultButton.setOnAction(null);

        this.playButton.setOnAction(null);
        this.pauseButton.setOnAction(null);
        this.newGameButton.setOnAction(null);
        getFrameTimer().stop();
    }

    private void prepareCanvas(Generation generation)
    {
        if(this.canvas.getWidth() != generation.getWidth())
            this.canvas.setWidth(generation.getWidth());
        if(this.canvas.getHeight() != generation.getHeight())
            this.canvas.setHeight(generation.getHeight());
    }

    private void init()
    {
        initBorderPane();
        initControls();
        layoutControls();
        initCanvas();
        initFrameTimer();
    }

    private void initBorderPane()
    {
        this.borderPane = new BorderPane();
        this.borderPane.setId("root-life");
    }

    private void initControls()
    {

        this.speedText = new Text();
        this.speedText.setId("text-speed");
        this.speedText.getStyleClass().add(TEXT_VALUE_CLASS);

        this.speedUpButton = new Button("+");
        this.speedUpButton.getStyleClass().add(BUTTON_PLUS_CLASS);
        this.speedDownButton = new Button("-");
        this.speedDownButton.getStyleClass().add(BUTTON_MINUS_CLASS);
        this.speedDefaultButton = new Button("default");
        this.speedDefaultButton.getStyleClass().add(BUTTON_DEFAULT_CLASS);

        this.zoomText = new Text();
        this.zoomText.setId("text-zoom");
        this.zoomText.getStyleClass().add(TEXT_VALUE_CLASS);

        this.zoomUpButton = new Button("+");
        this.zoomUpButton.getStyleClass().add(BUTTON_PLUS_CLASS);
        this.zoomDownButton = new Button("-");
        this.zoomDownButton.getStyleClass().add(BUTTON_MINUS_CLASS);
        this.zoomDefaultButton = new Button("default");
        this.zoomDefaultButton.getStyleClass().add(BUTTON_DEFAULT_CLASS);

        this.playButton = new Button("Play");
        this.playButton.setId("button-play");

        this.pauseButton = new Button("Pause");
        this.pauseButton.setId("button-pause");

        this.newGameButton = new Button("New Game");
        this.newGameButton.setId("button-new-game");

        this.generationNumberText = new Text();
        this.generationNumberText.setId("text-generation-number");
        this.generationNumberText.getStyleClass().add(TEXT_VALUE_CLASS);

        this.tipText = new Text();
        this.tipText.setId("text-tip");
        this.tipText.getStyleClass().add(TEXT_VALUE_CLASS);

        this.statusText = new Text();
        this.statusText.setId("text-status");
        this.statusText.getStyleClass().add(TEXT_VALUE_CLASS);
    }

    private void layoutControls()
    {
        HBox ctrls = new HBox
        (
            newLabelText("Speed"),
            this.speedDownButton, this.speedText,
            newButtonGroup(this.speedUpButton, this.speedDefaultButton),
            newSeparator(Orientation.VERTICAL),

            newLabelText("Zoom"),
            this.zoomDownButton, this.zoomText,
            newButtonGroup(this.zoomUpButton, this.zoomDefaultButton),
            newSeparator(Orientation.VERTICAL),

            newButtonGroup(this.newGameButton, this.pauseButton, this.playButton)
        );
        ctrls.setId("box-control");
        HBox info = new HBox
        (
            newLabelText("Generation:"), this.generationNumberText,
            newSeparator(Orientation.VERTICAL),
            newLabelText("Status:"), this.statusText,
            newSeparator(Orientation.VERTICAL),
            this.tipText
        );
        info.setId("box-info");
        VBox allCtrls = new VBox(ctrls, info);
        allCtrls.setId("box-all");
        this.borderPane.setTop(allCtrls);
    }

    private Separator newSeparator(Orientation orientation)
    {
        Separator result = new Separator(orientation);
        result.getStyleClass().add(SEPARATOR_CLASS);
        return result;
    }

    private Text newLabelText(String content)
    {
        Text result = new Text(content);
        result.getStyleClass().add(TEXT_LABEL_CLASS);
        return result;
    }

    private HBox newButtonGroup(Button... buttons)
    {
        HBox box = new HBox(buttons);
        box.getStyleClass().add(GROUP_BUTTON_CLASS);
        return box;
    }

    private void initCanvas()
    {
        this.canvas = new Canvas();
        this.canvas.getGraphicsContext2D().setFill(DEAD_COLOR);
        this.canvasGroup = new Group(canvas);
        this.canvasPane = new StackPane(this.canvasGroup);
        this.canvasGroup.layoutBoundsProperty().addListener
        (
            (o, ov, nv)->
            {
                this.canvasPane.setMinWidth(nv.getWidth());
                this.canvasPane.setMinHeight(nv.getHeight());
            }
        );

        this.canvasScroll = new ScrollPane(this.canvasPane);
        this.canvasScroll.setPannable(true);
        this.canvasScroll.viewportBoundsProperty().addListener
        (
            (o, ov, nv)->
            {
                this.canvasPane.setPrefWidth(nv.getWidth());
                this.canvasPane.setPrefHeight(nv.getHeight());
            }
        );

        this.borderPane.setCenter(this.canvasScroll);
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

    private void scaleCanvas(double factor, int pivotX, int pivotY)
    {
        if (factor == 0.0)
        {
            this.canvas.setScaleX(1);
            this.canvas.setScaleY(1);
            return;
        }

        Bounds groupBounds = this.canvasGroup.getLayoutBounds();
        Bounds viewportBounds = canvasScroll.getViewportBounds();

        double valX = this.canvasScroll.getHvalue()
            * (groupBounds.getWidth() - viewportBounds.getWidth());
        double valY = this.canvasScroll.getVvalue()
            * (groupBounds.getHeight() - viewportBounds.getHeight());

        Point2D posInZoomTarget = this.canvas
            .parentToLocal(this.canvasGroup.parentToLocal(new Point2D(pivotX, pivotY)));

        Point2D adjustment = this.canvas.getLocalToParentTransform()
            .deltaTransform(posInZoomTarget.multiply(factor - 1));

        this.canvas.setScaleX(factor * this.canvas.getScaleX());
        this.canvas.setScaleY(factor * this.canvas.getScaleY());

        this.canvasScroll.layout();

        groupBounds = this.canvasGroup.getLayoutBounds();
        this.canvasScroll.setHvalue
        (
            (valX + adjustment.getX())
            / (groupBounds.getWidth() - viewportBounds.getWidth())
        );
        this.canvasScroll.setVvalue
        (
            (valY + adjustment.getY())
            / (groupBounds.getHeight() - viewportBounds.getHeight())
        );
    }

}
