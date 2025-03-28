/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.io.ByteSource;
import java.io.File;
import java.io.IOException;
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
public class FileBackedOutputStream extends OutputStream {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackedOutputStream.class);

    /**
     * A Cleaner instance responsible for deleting any files which may be lost due to us not being cleaning up
     * temporary files.
     */
    private static final Cleaner FILE_CLEANER = Cleaner.create();

    private final int fileThreshold;
    private final Path fileDirectory;

    @GuardedBy("this")
    private MemoryOutputStream memory = new MemoryOutputStream();

    @GuardedBy("this")
    private OutputStream out = memory;

    @GuardedBy("this")
    private File file;

    @GuardedBy("this")
    private Cleanable fileCleanup;

    @GuardedBy("this")
    private DataSource source;

    @GuardedBy("this")
    private long count;

    /**
     * Default constructor. Resulting instance uses the given file threshold, and does not reset the data when the
     * {@link ByteSource} returned by {@link #asByteSource} is finalized.
     *
     * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
     * @param fileDirectory the directory in which to create the file if needed. If {@code null}, the default temp file
     *                      location is used.
     */
    // FIXME: java.io.Path
    public FileBackedOutputStream(final int fileThreshold, final @Nullable String fileDirectory) {
        this.fileThreshold = fileThreshold;
        this.fileDirectory = fileDirectory != null ? Path.of(fileDirectory) : null;
    }

    /**
     * Default constructor. Resulting instance uses the given file threshold, and does not reset the data when the
     * {@link ByteSource} returned by {@link #asByteSource} is finalized.
     *
     * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
     */
    public FileBackedOutputStream(final int fileThreshold) {
        this(fileThreshold, null);
    }

    /**
     * Returns a readable {@link DataSource} view of the data that has been written to this stream. This stream is
     * closed and further attempts to write to it will result in an IOException.
     *
     * @return a {@link DataSource} instance
     * @throws IOException if close fails
     */
    public synchronized @NonNull DataSource asDataSource() throws IOException {
        close();

        var local = source;
        if (local == null) {
            final var lf = file;
            source = local = lf != null ? new FileDataSource(lf.toPath()) : memory.toDataSource();
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

        if (file == null && memory.count() + len > fileThreshold) {
            final var temp = File.createTempFile("FileBackedOutputStream", null,
                    fileDirectory == null ? null : fileDirectory.toFile());
            temp.deleteOnExit();
            final var cleanable = FILE_CLEANER.register(this, () -> deleteFile(temp));

            LOG.debug("Byte count {} has exceeded threshold {} - switching to file: {}", memory.count() + len,
                    fileThreshold, temp);

            final OutputStream transfer;
            try {
                transfer = Files.newOutputStream(temp.toPath());
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
    }

    private static void deleteFile(final File file) {
        LOG.debug("Deleting temp file {}", file);
        if (!file.delete()) {
            LOG.warn("Could not delete temp file {}", file);
        }
    }
}
