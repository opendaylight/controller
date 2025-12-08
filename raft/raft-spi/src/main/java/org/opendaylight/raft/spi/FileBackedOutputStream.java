/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.io.CountingOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link OutputStream} that starts buffering to a byte array, but switches to file buffering once the data
 * reaches a configurable size. This class is thread-safe.
 *
 * @author Thomas Pantelis
 */
// Non-sealed for testing
public class FileBackedOutputStream extends OutputStream {
    /**
     * Configuration for {@link FileBackedOutputStream}.
     *
     * @param threshold the number of bytes before the stream should switch to buffering to a file
     * @param directory the directory in which to create the file if needed. If {@code null}, the default temp file
     *                      location is used.
     */
    public record Configuration(int threshold, @Nullable Path directory) {
        // Nothing else
    }

    @NonNullByDefault
    @FunctionalInterface
    private interface TempFileCreator {

        Path newTempFile() throws IOException;
    }

    @NonNullByDefault
    private sealed interface State permits Closed, Open {

        long count();
    }

    @NonNullByDefault
    private sealed interface WithFile permits ClosedFile, OpenFile {

        TransientFile file();
    }

    @NonNullByDefault
    private record Cleaned(Path file, long count) implements Closed {
        Cleaned {
            requireNonNull(file);
        }
    }

    @NonNullByDefault
    private sealed interface Closed extends State permits Cleaned, ClosedFile, ClosedMemory {
        // Nothing else
    }

    @NonNullByDefault
    private record ClosedFile(TransientFile file, long count) implements Closed, WithFile {
        ClosedFile {
            requireNonNull(file);
        }
    }

    @NonNullByDefault
    private record ClosedMemory(MemoryStream memory) implements Closed {
        @Override
        public long count() {
            return memory.count();
        }
    }

    @NonNullByDefault
    private sealed interface Open extends State permits OpenFile, OpenMemory {

        OutputStream out();

        Closed toClosed() throws IOException;
    }

    @NonNullByDefault
    private record OpenFile(CountingOutputStream out, TransientFile file) implements Open, WithFile {
        OpenFile {
            requireNonNull(out);
            requireNonNull(file);
        }

        @Override
        public long count() {
            return out.getCount();
        }

        @Override
        public ClosedFile toClosed() throws IOException {
            out.close();
            return new ClosedFile(file, out.getCount());
        }
    }

    @NonNullByDefault
    private record OpenMemory(MemoryStream out) implements Open {
        OpenMemory {
            requireNonNull(out);
        }

        @Override
        public long count() {
            return out.count();
        }

        @Override
        public ClosedMemory toClosed() {
            return new ClosedMemory(out);
        }
    }

    // For un-synchronized access to count/buffer
    @NonNullByDefault
    private static final class MemoryStream extends ByteArrayOutputStream {
        void transferTo(final OutputStream out) throws IOException {
            out.write(buf, 0, count);
            out.flush();
        }

        int count() {
            return count;
        }

        MemoryStreamSource toStreamSource() {
            return new MemoryStreamSource(buf, count);
        }
    }

    @NonNullByDefault
    private record MemoryStreamSource(byte[] bytes, int count) implements SizedStreamSource {
        @Override
        public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(bytes, 0, count);
        }

        @Override
        public long size() {
            return count;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("size", count).toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileBackedOutputStream.class);

    private final TempFileCreator fileCreator;
    private final int threshold;

    private @GuardedBy("this") State state = new OpenMemory(new MemoryStream());
    private @GuardedBy("this") SizedStreamSource source;

    /**
     * Default constructor. Resulting instance uses the given file threshold, and does not reset the data when the
     * {@link SizedStreamSource} returned by {@link #toStreamSource()} is finalized.
     *
     * @param config the {@link Configuration} to use
     */
    public FileBackedOutputStream(final Configuration config) {
        threshold = config.threshold;

        final var dir = config.directory;
        fileCreator = dir == null ? () -> Files.createTempFile("FileBackedOutputStream", null)
            : () -> Files.createTempFile(dir, "FileBackedOutputStream", null);
    }

    /**
     * Returns a readable {@link SizedStreamSource} view of the data that has been written to this stream. This stream
     * is closed and further attempts to write to it will result in an IOException.
     *
     * @return a SizedStreamSource instance
     * @throws IOException if close fails
     */
    public synchronized @NonNull SizedStreamSource toStreamSource() throws IOException {
        final Closed closed;
        switch (state) {
            case Closed already -> closed = already;
            case Open open -> state = closed = open.toClosed();
        }

        var local = source;
        if (local != null) {
            return local;
        }

        source = local = switch (closed) {
            case Cleaned cleaned -> throw new IOException("Reference to " + cleaned.file + " already cleaned");
            case ClosedFile file -> new TransientFileStreamSource(file.file, 0, file.count);
            case ClosedMemory(var memory) -> memory.toStreamSource();
        };
        return local;
    }

    @Override
    public synchronized void write(final int value) throws IOException {
        ensureCapacity(1).write(value);
    }

    @Override
    public synchronized void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void write(final byte[] bytes, final int off, final int len) throws IOException {
        ensureCapacity(len).write(bytes, off, len);
    }

    @Override
    public synchronized void close() throws IOException {
        if (state instanceof Open open) {
            state = open.toClosed();
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        if (state instanceof Open open) {
            open.out().flush();
        }
    }

    /**
     * Returns current byte size.
     *
     * @return current byte size
     */
    public synchronized long getCount() {
        return state.count();
    }

    /**
     * Calls {@link #close} if not already closed and, if data was buffered to a file, deletes the file.
     */
    // FIXME: decRef()?
    public synchronized void cleanup() {
        LOG.debug("In cleanup");

        var local = state;
        if (local instanceof Open open) {
            try {
                state = local = open.toClosed();
            } catch (IOException e) {
                LOG.warn("Error closing output stream {}", open.out(), e);
            }
        }
        if (local instanceof WithFile withFile) {
            source = null;
            final var file = withFile.file();
            file.delete();
            state = new Cleaned(file.path(), local.count());
        }
    }

    @Holding("this")
    private @NonNull OutputStream ensureCapacity(final int len) throws IOException {
        return switch (state) {
            case Closed closed -> throw new IOException("Stream already closed");
            case OpenFile open -> open.out;
            case OpenMemory(final var out) -> {
                // if writing {@code len} bytes would go over threshold, and switches to file buffering if so.
                final var newLength = out.count() + len;
                yield newLength <= threshold ? out : switchToFile(out, newLength);
            }
        };
    }

    @Holding("this")
    private @NonNull CountingOutputStream switchToFile(final MemoryStream oldOut, final int newLength)
            throws IOException {
        final var file = new TransientFile(fileCreator.newTempFile());

        LOG.debug("Byte count {} has exceeded threshold {} - switching to file: {}", newLength, threshold, file.path());

        final CountingOutputStream newOut;
        try {
            newOut = new CountingOutputStream(Files.newOutputStream(file.path()));
            try {
                oldOut.transferTo(newOut);
            } catch (IOException e) {
                try {
                    newOut.close();
                } catch (IOException ex) {
                    LOG.debug("Error closing temp file {}", file.path(), ex);
                }
                throw e;
            }
        } catch (IOException e) {
            file.delete();
            throw e;
        }

        // We've successfully transferred the data; switch to writing to file
        state = new OpenFile(newOut, file);
        return newOut;
    }
}
