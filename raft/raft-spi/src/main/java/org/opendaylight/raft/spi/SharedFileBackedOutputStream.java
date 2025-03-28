/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A FileBackedOutputStream that allows for sharing in that it maintains a usage count and the backing file isn't
 * deleted until the usage count reaches 0. The usage count is initialized to 1 on construction. Subsequent users of
 * the instance must call {@link #incrementUsageCount()}. The {@link #cleanup()} method decrements the usage count and,
 * when it reaches 0, the {@link FileBackedOutputStream#cleanup()} is called to delete the backing file.
 *
 * @author Thomas Pantelis
 */
public final class SharedFileBackedOutputStream extends FileBackedOutputStream {
    private final AtomicInteger usageCount = new AtomicInteger(1);

    // FIXME: err... what?
    @SuppressWarnings("rawtypes")
    private Consumer onCleanupCallback;
    private Object onCleanupContext;

    /**
     * Default constructor. Resulting instance uses the given file threshold, and does not reset the data when the
     * {@link ByteSource} returned by {@link #asByteSource} is finalized.
     *
     * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
     * @param fileDirectory the directory in which to create the file if needed. If {@code null}, the default temp file
     *                      location is used.
     */
    public SharedFileBackedOutputStream(final int fileThreshold, final Path fileDirectory) {
        super(fileThreshold, fileDirectory);
    }

    /**
     * Increments the usage count. This must be followed by a corresponding call to {@link #cleanup()} when this
     * instance is no longer needed.
     */
    // FIXME: incRef() ... do we want this to be a ByteBuf?
    public void incrementUsageCount() {
        usageCount.getAndIncrement();
    }

    /**
     * Returns the current usage count.
     *
     * @return the current usage count
     */
    // FIXME: refCount()
    public int getUsageCount() {
        return usageCount.get();
    }

    /**
     * Sets the callback to be notified when {@link FileBackedOutputStream#cleanup()} is called to delete the backing
     * file.
     *
     * @param <T> context type
     * @param callback the callback
     * @param context the context
     */
    // FIXME: just a Runnable
    public <T> void setOnCleanupCallback(final Consumer<T> callback, final T context) {
        onCleanupCallback = callback;
        onCleanupContext = context;
    }

    /**
     * Overridden to decrement the usage count.
     */
    // FIXME: decRef(), really
    @SuppressWarnings("unchecked")
    @Override
    public void cleanup() {
        Preconditions.checkState(usageCount.get() > 0);

        if (usageCount.decrementAndGet() == 0) {
            super.cleanup();

            if (onCleanupCallback != null) {
                onCleanupCallback.accept(onCleanupContext);
            }
        }
    }
}
