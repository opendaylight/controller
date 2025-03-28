/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Support for opening {@link InputStream}s -- be it {@link #simple()} or {@link #lz4(Lz4BlockSize)}.
 */
public abstract sealed class InputOutputStreamFactory
        permits LZ4InputOutputStreamSupport, PlainInputOutputStreamSupport {
    InputOutputStreamFactory() {
        // Hidden on purpose
    }

    /**
     * Returns a plain {@link InputOutputStreamFactory}.
     *
     * @return a plain {@link InputOutputStreamFactory}
     */
    public static @NonNull InputOutputStreamFactory simple() {
        return PlainInputOutputStreamSupport.INSTANCE;
    }

    /**
     * Returns an LZ4 {@link InputOutputStreamFactory}.
     *
     * @param blockSize requested block size
     * @return an LZ4 {@link InputOutputStreamFactory}
     */
    public static @NonNull InputOutputStreamFactory lz4(final Lz4BlockSize blockSize) {
        return new LZ4InputOutputStreamSupport(requireNonNull(blockSize));
    }

    /**
     * Create a new {@link InputStream}.
     *
     * @param input source of backing input stream
     * @return an {@link InputStream}
     * @throws IOException if an I/O error occurs
     */
    public abstract @NonNull InputStream createInputStream(DataSource input) throws IOException;

    /**
     * Create a new {@link OutputStream}.
     *
     * @param file source of backing input stream
     * @return an {@link OutputStream}
     * @throws IOException if an I/O error occurs
     */
    public abstract @NonNull OutputStream createOutputStream(File file) throws IOException;

    /**
     * Create wraps an existing {@link OutputStream}.
     *
     * @param output the stream to wrap
     * @return an {@link OutputStream}
     * @throws IOException if an I/O error occurs
     */
    @NonNullByDefault
    public abstract OutputStream wrapOutputStream(OutputStream output) throws IOException;

    @NonNullByDefault
    static final InputStream ensureBuffered(final InputStream stream) {
        return switch (stream) {
            case BufferedInputStream bis -> bis;
            case ByteArrayInputStream bais -> bais;
            default -> new BufferedInputStream(stream);
        };
    }

    static final @NonNull BufferedInputStream defaultCreateInputStream(final Path file) throws IOException {
        return new BufferedInputStream(Files.newInputStream(file));
    }

    static final @NonNull BufferedOutputStream defaultCreateOutputStream(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file));
    }
}
