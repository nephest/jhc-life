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

import com.nephest.jhclife.*;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import javafx.application.*;
import javafx.scene.layout.Region;
import javafx.scene.control.*;
import javafx.scene.*;
import javafx.stage.*;

public abstract class ViewBaseImplFX
implements ViewBase<Parent>
{

    private final Window ownerWindow;

    private Alert stdAlert;
    private Alert confirmationAlert;
    private final FileChooser fileChooser = new FileChooser();

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
                this.stdAlert.setTitle(GUILauncherFX.MAIN_TITLE_PREFIX + "Information");
                this.stdAlert.setHeaderText(header);
                this.stdAlert.setContentText(text);
                this.stdAlert.showAndWait();
            }
        );
    }

    @Override
    public void fireConfirmationAlert
    (
        String header,
        String text,
        Runnable onYes,
        Runnable onNo
    )
    {
        Platform.runLater
        (
            ()->
            {
                this.confirmationAlert.setTitle(GUILauncherFX.MAIN_TITLE_PREFIX + "Confirmation");
                this.confirmationAlert.setHeaderText(header);
                this.confirmationAlert.setContentText(text);
                this.confirmationAlert.showAndWait().ifPresent
                (
                    (buttonType)->
                    {
                        if (buttonType == ButtonType.YES && onYes != null)
                        {
                            onYes.run();
                        }
                        else if (buttonType == ButtonType.NO && onNo != null)
                        {
                            onNo.run();
                        }
                    }
                );
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
                this.stdAlert.setTitle(GUILauncherFX.MAIN_TITLE_PREFIX + "Error");
                this.stdAlert.setHeaderText(header);
                this.stdAlert.setContentText(text);
                this.stdAlert.showAndWait();
            }
        );
    }

    @Override
    public void selectFile
    (
        ViewBase.FileSelectionMode mode,
        String title,
        String initialName,
        Consumer<List<File>> onSelect
    )
    {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(onSelect);
        if (getTopWindow() == null)
            throw new IllegalStateException("the root node is not attached to a window");
        Platform.runLater(()->doSelectFile(mode, title, initialName, onSelect));
    }

    private void doSelectFile
    (
        ViewBase.FileSelectionMode mode,
        String title,
        String initialName,
        Consumer<List<File>> onSelect
    )
    {
        this.fileChooser.setTitle(title);
        this.fileChooser.setInitialFileName(initialName);

        List<File> files = new ArrayList();
        switch(mode)
        {
            case SELECT_SINGLE:
            {
                File file = this.fileChooser.showOpenDialog(getTopWindow());
                if (file != null) files.add(file);
                break;
            }
            case SELECT_MULTIPLE:
            {
                List<File> filesSrc = this.fileChooser.showOpenMultipleDialog(getTopWindow());
                if (filesSrc != null) files.addAll(filesSrc);
                break;
            }
            case SAVE:
            {
                File file = this.fileChooser.showSaveDialog(getTopWindow());
                if (file != null) files.add(file);
                break;
            }
        }
        onSelect.accept(files);
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

        this.stdAlert.getDialogPane().setId("alert-standard");
        this.confirmationAlert.getDialogPane().setId("alert-confirm");

        this.stdAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        this.confirmationAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    }

    public Window getOwnerWindow()
    {
        return this.ownerWindow;
    }

    public Window getTopWindow()
    {
        return getRoot().getScene() != null
            ? getRoot().getScene().getWindow()
            : null;
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
