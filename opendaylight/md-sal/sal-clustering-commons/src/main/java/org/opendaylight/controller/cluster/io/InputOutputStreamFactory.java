/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.io.ByteSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.eclipse.jdt.annotation.NonNull;

@Beta
public abstract class InputOutputStreamFactory {
    InputOutputStreamFactory() {
        // Hidden on purpose
    }

    public static @NonNull InputOutputStreamFactory simple() {
        return PlainInputOutputStreamSupport.INSTANCE;
    }

    public static @NonNull InputOutputStreamFactory lz4(final String blockSize) {
        return lz4(LZ4FrameOutputStream.BLOCKSIZE.valueOf("SIZE_" + blockSize));
    }

    public static @NonNull InputOutputStreamFactory lz4(final LZ4FrameOutputStream.BLOCKSIZE blockSize) {
        return new LZ4InputOutputStreamSupport(requireNonNull(blockSize));
    }

    public abstract @NonNull InputStream createInputStream(ByteSource input) throws IOException;

    public abstract @NonNull InputStream createInputStream(File file) throws IOException;

    public abstract @NonNull OutputStream createOutputStream(File file) throws IOException;

    public abstract @NonNull OutputStream wrapOutputStream(OutputStream output) throws IOException;

    static @NonNull BufferedInputStream defaultCreateInputStream(final File file) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    static @NonNull BufferedOutputStream defaultCreateOutputStream(final File file) throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(file));
    }
}
