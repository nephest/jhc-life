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

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface ViewBase<N>
{

    public static enum AlertType
    {
        INFO, ERROR;
    }

    public static enum FileSelectionMode
    {
        SELECT_SINGLE, SELECT_MULTIPLE, SAVE;
    }

    public void fireAlert(AlertType type, String header, String text);

    public void fireInfoAlert(String header, String text);

    public void fireConfirmationAlert
    (
        String header,
        String text,
        Runnable onYes,
        Runnable onNo
    );

    public void fireErrorAlert(String header, String text);

    public void selectFile
    (
        FileSelectionMode mode,
        String title,
        String initialName,
        Consumer<List<File>> onSelect
    );

    public void lock();

    public void unlock();

    public boolean isLocked();

    public N getRoot();

}
