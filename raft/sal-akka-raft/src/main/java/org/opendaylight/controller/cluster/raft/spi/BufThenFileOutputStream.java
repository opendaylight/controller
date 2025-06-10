/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.spi.TransientFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link OutputStream} which stats off writing to a ByteBuf, but switches to a file.
 */
final class BufThenFileOutputStream extends OutputStream {
    private sealed interface State {
        // Nothing else
    }

    private record ToBuf(ByteBuf buf, int startIndex, int threshold) implements State {
        ToBuf {
            if (threshold < 1) {
                throw new IllegalArgumentException("Invalid threshold " + threshold);
            }
            final var avail = buf.writableBytes();
            if (avail < threshold) {
                throw new IllegalArgumentException("Insufficient capacity " + avail + " with threshold " + threshold);
            }
        }
    }

    private record ToFile(TransientFile file, BufferedOutputStream out) implements State {
        ToFile {
            requireNonNull(file);
            requireNonNull(out);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BufThenFileOutputStream.class);

    private final Path directory;

    private State state;
    private long size = 0;

    BufThenFileOutputStream(final Path directory, final ByteBuf buf, final int threshold) {
        this.directory = requireNonNull(directory);
        state = new ToBuf(buf, buf.writerIndex(), threshold);
    }

    @Nullable TransientFile file() {
        return state instanceof ToFile toFile ? toFile.file : null;
    }

    long size() {
        return size;
    }

    @Override
    public void write(final int value) throws IOException {
        final var next = size + 1;
        switch (state) {
            case ToBuf toBuf -> {
                if (next > toBuf.threshold) {
                    switchToFile(toBuf.buf, toBuf.startIndex).write(value);
                } else {
                    toBuf.buf.writeByte(value);
                }
            }
            case ToFile toFile -> toFile.out.write(value);
        }
        size = next;
    }

    @Override
    public void write(final byte[] bytes, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, bytes.length);
        final var next = size + len;
        switch (state) {
            case ToBuf toBuf -> {
                if (next > toBuf.threshold) {
                    switchToFile(toBuf.buf, toBuf.startIndex).write(bytes, off, len);
                } else {
                    toBuf.buf.writeBytes(bytes, off, len);
                }
            }
            case ToFile toFile -> toFile.out.write(bytes, off, len);
        }
        size = next;
    }

    @Override
    public void flush() throws IOException {
        if (state instanceof ToFile toFile) {
            toFile.out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (state instanceof ToFile toFile) {
            toFile.out.close();
        }
    }

    @NonNullByDefault
    private OutputStream switchToFile(final ByteBuf buf, final int startIndex) throws IOException {
        LOG.debug("Switching to file after {} bytes", size);

        final var file = new TransientFile(Files.createTempFile(directory, "BufThenFileOutputStream", null));
        final BufferedOutputStream newOut;
        try {
            newOut = new BufferedOutputStream(Files.newOutputStream(file.path()));
            try {
                buf.getBytes(startIndex, newOut, buf.writerIndex() - startIndex);
            } catch (IOException e) {
                try {
                    newOut.close();
                } catch (IOException ce) {
                    LOG.debug("Error closing temp file {}", file.path(), ce);
                }
                throw e;
            }
            buf.writerIndex(startIndex);
        } catch (IOException e) {
            file.delete();
            throw e;
        }

        state = new ToFile(file, newOut);
        return newOut;
    }
}
