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
import javafx.scene.Node;

public abstract class ViewBaseImplFX<Parent>
implements ViewBase<Parent>
{

    private Alert errorWarningAlert;
    private Alert confirmationAlert;

    private Node lockable;
    private boolean locked = false;

    public ViewBaseImplFX()
    {
        init();
    }

    @Override
    public void fireErrorAlert(String text)
    {
        Platform.runLater
        (
            ()->
            {
                this.errorWarningAlert.setAlertType(Alert.AlertType.ERROR);
                this.errorWarningAlert.setContentText(text);
                this.errorWarningAlert.showAndWait();
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
        this.errorWarningAlert
            = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
        this.confirmationAlert = new Alert
        (
            Alert.AlertType.CONFIRMATION,
            "",
            ButtonType.YES, ButtonType.NO
        );

        this.errorWarningAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        this.confirmationAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }

}