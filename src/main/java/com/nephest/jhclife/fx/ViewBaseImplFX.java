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

import java.util.Objects;

import javafx.application.*;
import javafx.scene.layout.Region;
import javafx.scene.control.*;
import javafx.scene.*;
import javafx.stage.Window;

public abstract class ViewBaseImplFX
implements ViewBase<Parent>
{

    private final Window ownerWindow;

    private Alert stdAlert;
    private Alert confirmationAlert;

    private Node lockable;
    private boolean locked = false;

    public ViewBaseImplFX(Window ownerWindow)
    {
        init();
        this.ownerWindow = ownerWindow;
        initOwnerWindow();
    }

    public ViewBaseImplFX()
    {
        this(null);
    }

    @Override
    public void fireAlert(ViewBase.AlertType type, String header, String text)
    {
        switch(type)
        {
            case ERROR:
                fireErrorAlert(header, text);
                break;
            case INFO:
                fireInfoAlert(header, text);
                break;
        }
    }

    @Override
    public void fireInfoAlert(String header, String text)
    {
        Platform.runLater
        (
            ()->
            {
                this.stdAlert.setAlertType(Alert.AlertType.INFORMATION);
                this.stdAlert.setTitle("Information");
                this.stdAlert.setHeaderText(header);
                this.stdAlert.setContentText(text);
                this.stdAlert.showAndWait();
            }
        );
    }

    @Override
    public void fireErrorAlert(String header, String text)
    {
        Platform.runLater
        (
            ()->
            {
                this.stdAlert.setAlertType(Alert.AlertType.ERROR);
                this.stdAlert.setTitle("Error");
                this.stdAlert.setHeaderText(header);
                this.stdAlert.setContentText(text);
                this.stdAlert.showAndWait();
            }
        );
    }

    @Override
    public void lock()
    {
        if (isLocked() || getLockable() == null) return;
        this.locked = true;
        Platform.runLater( ()->getLockable().setDisable(true) );

    }

    @Override
    public void unlock()
    {
        if (!isLocked() || getLockable() == null) return;
        this.locked = false;
        Platform.runLater( ()->getLockable().setDisable(false) );
    }

    @Override
    public boolean isLocked()
    {
        return this.locked;
    }

    protected void setLockable(Node node)
    {
        Objects.requireNonNull(node);
        this.lockable = node;
    }

    private Node getLockable()
    {
        return this.lockable;
    }

    private void init()
    {
        initAlerts();
    }

    private void initAlerts()
    {
        this.stdAlert
            = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
        this.confirmationAlert = new Alert
        (
            Alert.AlertType.CONFIRMATION,
            "",
            ButtonType.YES, ButtonType.NO
        );

        this.stdAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        this.confirmationAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }

    public Window getOwnerWindow()
    {
        return this.ownerWindow;
    }

    private void initOwnerWindow()
    {
        if(getOwnerWindow() != null)
        {
            this.stdAlert.initOwner(getOwnerWindow());
            this.confirmationAlert.initOwner(getOwnerWindow());
        }
    }

    public void addExternalElementsCss(String resource)
    {
        Objects.requireNonNull(resource);

        this.stdAlert.getDialogPane().getStylesheets().add(resource);
        this.confirmationAlert.getDialogPane().getStylesheets().add(resource);
    }

    public void removeExternalElementsCss(String resource)
    {
        Objects.requireNonNull(resource);

        this.stdAlert.getDialogPane().getStylesheets().remove(resource);
        this.confirmationAlert.getDialogPane().getStylesheets().remove(resource);
    }

}
