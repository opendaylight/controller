/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
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
@ThreadSafe
public class FileBackedOutputStream extends OutputStream {
    private static final Logger LOG = LoggerFactory.getLogger(FileBackedOutputStream.class);

    /**
     * This stores the Cleanup PhantomReference instances statically. This is necessary because PhantomReferences
     * need a hard reference so they're not garbage collected. Once finalized, the Cleanup PhantomReference removes
     * itself from this map and thus becomes eligible for garbage collection.
     */
    @VisibleForTesting
    static final Set<Cleanup> REFERENCE_CACHE = Sets.newConcurrentHashSet();

    /**
     * Used as the ReferenceQueue for the Cleanup PhantomReferences.
     */
    private static final FinalizableReferenceQueue REFERENCE_QUEUE = new FinalizableReferenceQueue();

    private final int fileThreshold;
    private final String fileDirectory;

    @GuardedBy("this")
    private MemoryOutputStream memory = new MemoryOutputStream();

    @GuardedBy("this")
    private OutputStream out = memory;

    @GuardedBy("this")
    private File file;

    @GuardedBy("this")
    private ByteSource source;

    private volatile long count;

    /**
     * Creates a new instance that uses the given file threshold, and does not reset the data when the
     * {@link ByteSource} returned by {@link #asByteSource} is finalized.
     *
     * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
     * @param fileDirectory the directory in which to create the file if needed. If null, the default temp file
     *                      location is used.
     */
    public FileBackedOutputStream(int fileThreshold, @Nullable String fileDirectory) {
        this.fileThreshold = fileThreshold;
        this.fileDirectory = fileDirectory;
    }

    /**
     * Returns a readable {@link ByteSource} view of the data that has been written to this stream. This stream is
     * closed and further attempts to write to it will result in an IOException.
     *
     * @return a ByteSource instance
     * @throws IOException if close fails
     */
    public synchronized @NonNull ByteSource asByteSource() throws IOException {
        close();

        if (source == null) {
            source = new ByteSource() {
                @Override
                public InputStream openStream() throws IOException {
                    synchronized (FileBackedOutputStream.this) {
                        if (file != null) {
                            return Files.newInputStream(file.toPath());
                        } else {
                            return new ByteArrayInputStream(memory.getBuffer(), 0, memory.getCount());
                        }
                    }
                }

                @Override
                public long size() {
                    return count;
                }
            };
        }

        return source;
    }

    @Override
    @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT", justification = "Findbugs erroneously complains that the "
        + "increment of count needs to be atomic even though it is inside a synchronized block.")
    public synchronized void write(int value) throws IOException {
        possiblySwitchToFile(1);
        out.write(value);
        count++;
    }

    @Override
    public synchronized void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void write(byte[] bytes, int off, int len) throws IOException {
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

    public synchronized long getCount() {
        return count;
    }

    /**
     * Calls {@link #close} if not already closed and, if data was buffered to a file, deletes the file.
     */
    public synchronized void cleanup() {
        LOG.debug("In cleanup");

        closeQuietly();

        if (file != null) {
            Iterator<Cleanup> iter = REFERENCE_CACHE.iterator();
            while (iter.hasNext()) {
                if (file.equals(iter.next().file)) {
                    iter.remove();
                    break;
                }
            }

            LOG.debug("cleanup - deleting temp file {}", file);

            deleteFile(file);
            file = null;
        }
    }

    @GuardedBy("this")
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
    @GuardedBy("this")
    private void possiblySwitchToFile(int len) throws IOException {
        if (out == null) {
            throw new IOException("Stream already closed");
        }

        if (file == null && memory.getCount() + len > fileThreshold) {
            File temp = File.createTempFile("FileBackedOutputStream", null,
                    fileDirectory == null ? null : new File(fileDirectory));
            temp.deleteOnExit();

            LOG.debug("Byte count {} has exceeded threshold {} - switching to file: {}", memory.getCount() + len,
                    fileThreshold, temp);

            OutputStream transfer = null;
            try {
                transfer = Files.newOutputStream(temp.toPath());
                transfer.write(memory.getBuffer(), 0, memory.getCount());
                transfer.flush();

                // We've successfully transferred the data; switch to writing to file
                out = transfer;
                file = temp;
                memory = null;

                new Cleanup(this, file);
            } catch (IOException e) {
                if (transfer != null) {
                    try {
                        transfer.close();
                    } catch (IOException ex) {
                        LOG.debug("Error closing temp file {}", temp, ex);
                    }
                }

                deleteFile(temp);
                throw e;
            }
        }
    }

    private static void deleteFile(File file) {
        if (!file.delete()) {
            LOG.warn("Could not delete temp file {}", file);
        }
    }

    /**
     * ByteArrayOutputStream that exposes its internals for efficiency.
     */
    private static class MemoryOutputStream extends ByteArrayOutputStream {
        byte[] getBuffer() {
            return buf;
        }

        int getCount() {
            return count;
        }
    }

    /**
     * PhantomReference that deletes the temp file when the FileBackedOutputStream is garbage collected.
     */
    private static class Cleanup extends FinalizablePhantomReference<FileBackedOutputStream> {
        private final File file;

        Cleanup(FileBackedOutputStream referent, File file) {
            super(referent, REFERENCE_QUEUE);
            this.file = file;

            REFERENCE_CACHE.add(this);

            LOG.debug("Added Cleanup for temp file {}", file);
        }

        @Override
        public void finalizeReferent() {
            LOG.debug("In finalizeReferent");

            if (REFERENCE_CACHE.remove(this)) {
                LOG.debug("finalizeReferent - deleting temp file {}", file);
                deleteFile(file);
            }
        }
    }
}
