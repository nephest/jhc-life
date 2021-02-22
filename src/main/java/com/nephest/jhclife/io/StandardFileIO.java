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

package com.nephest.jhclife.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public class StandardFileIO
implements FileIO
{

    @Override
    public OutputStream getOutputStream(File file)
    throws FileNotFoundException
    {
        return new FileOutputStream(file);
    }

    @Override
    public void write(Path path, byte[] bytes, OpenOption... options)
    throws IOException
    {
        Files.write(path, bytes, options);
    }

    @Override
    public byte[] readAllBytes(Path path)
    throws IOException
    {
        return Files.readAllBytes(path);
    }

}
