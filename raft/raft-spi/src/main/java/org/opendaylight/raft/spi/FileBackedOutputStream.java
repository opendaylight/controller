/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
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

    @FunctionalInterface
    private interface TempFileCreator {

        Path newTempFile() throws IOException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileBackedOutputStream.class);

    /**
     * A Cleaner instance responsible for deleting any files which may be lost due to us not being cleaning up
     * temporary files.
     */
    private static final Cleaner FILE_CLEANER = Cleaner.create();

    private final TempFileCreator fileCreator;
    private final int threshold;

    @GuardedBy("this")
    private MemoryOutputStream memory = new MemoryOutputStream();
    @GuardedBy("this")
    private OutputStream out = memory;
    @GuardedBy("this")
    private Path file;
    @GuardedBy("this")
    private Cleanable fileCleanup;
    @GuardedBy("this")
    private SizedStreamSource source;
    @GuardedBy("this")
    private long count;

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
        close();

        var local = source;
        if (local == null) {
            // Note: needs to retain reference to 'this' so as to keep the cleaner being invoked. we really should
            //       have that taken careof by having a live object encapsulating the file
            source = local = new SizedStreamSource() {
                @Override
                public InputStream openStream() throws IOException {
                    synchronized (FileBackedOutputStream.this) {
                        return file != null ? Files.newInputStream(file)
                            // FIXME: we would be able to use ByteArray.wrap() or similar
                            : new ByteArrayInputStream(memory.buf(), 0, memory.count());
                    }
                }

                @Override
                public InputStream openBufferedStream() throws IOException {
                    final var stream = openStream();
                    return stream instanceof ByteArrayInputStream baos ? baos : new BufferedInputStream(stream);
                }

                @Override
                public long size() {
                    synchronized (FileBackedOutputStream.this) {
                        return count;
                    }
                }
            };
        }

        return local;
    }

    @Override
    public synchronized void write(final int value) throws IOException {
        possiblySwitchToFile(1);
        out.write(value);
        count++;
    }

    @Override
    public synchronized void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void write(final byte[] bytes, final int off, final int len) throws IOException {
        possiblySwitchToFile(len);
        out.write(bytes, off, len);
        count += len;
    }

    @Override
    public synchronized void close() throws IOException {
        if (out != null) {
            OutputStream closeMe = out;
            out = null;
            closeMe.close();
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /**
     * Returns current reference count.
     *
     * @return current reference count
     */
    // FIXME: refCount()
    public synchronized long getCount() {
        return count;
    }

    /**
     * Calls {@link #close} if not already closed and, if data was buffered to a file, deletes the file.
     */
    // FIXME: decRef()?
    public synchronized void cleanup() {
        LOG.debug("In cleanup");
        closeQuietly();
        if (fileCleanup != null) {
            fileCleanup.clean();
        }
        // Already deleted above
        file = null;
    }

    @Holding("this")
    private void closeQuietly() {
        try {
            close();
        } catch (IOException e) {
            LOG.warn("Error closing output stream {}", out, e);
        }
    }

    /**
     * Checks if writing {@code len} bytes would go over threshold, and switches to file buffering if so.
     */
    @Holding("this")
    private void possiblySwitchToFile(final int len) throws IOException {
        if (out == null) {
            throw new IOException("Stream already closed");
        }

        if (file == null) {
            final var newLength = memory.count() + len;
            if (newLength > threshold) {
                switchToFile(newLength);
            }
        }
    }

    private void switchToFile(final int newLength) throws IOException {
        final var temp = fileCreator.newTempFile();
        temp.toFile().deleteOnExit();
        final var cleanable = FILE_CLEANER.register(this, () -> {
            LOG.debug("Deleting temp file {}", temp);
            try {
                Files.delete(temp);
            } catch (IOException e) {
                LOG.warn("Could not delete temp file {}", temp, e);
            }
        });

        LOG.debug("Byte count {} has exceeded threshold {} - switching to file: {}", newLength, threshold, temp);

        final OutputStream transfer;
        try {
            transfer = Files.newOutputStream(temp);
            try {
                transfer.write(memory.buf(), 0, memory.count());
                transfer.flush();
            } catch (IOException e) {
                try {
                    transfer.close();
                } catch (IOException ex) {
                    LOG.debug("Error closing temp file {}", temp, ex);
                }
                throw e;
            }
        } catch (IOException e) {
            cleanable.clean();
            throw e;
        }

        // We've successfully transferred the data; switch to writing to file
        out = transfer;
        file = temp;
        fileCleanup = cleanable;
        memory = null;
    }

    /**
     * ByteArrayOutputStream that exposes its internals for efficiency.
     */
    private static final class MemoryOutputStream extends ByteArrayOutputStream {
        byte[] buf() {
            return buf;
        }

        int count() {
            return count;
        }
    }
}
