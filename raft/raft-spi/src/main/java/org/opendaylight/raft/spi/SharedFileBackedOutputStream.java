/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A FileBackedOutputStream that allows for sharing in that it maintains a usage count and the backing file isn't
 * deleted until the usage count reaches 0. The usage count is initialized to 1 on construction. Subsequent users of
 * the instance must call {@link #incrementUsageCount()}. The {@link #cleanup()} method decrements the usage count and,
 * when it reaches 0, the {@link FileBackedOutputStream#cleanup()} is called to delete the backing file.
 *
 * @author Thomas Pantelis
 */
public final class SharedFileBackedOutputStream extends FileBackedOutputStream {
    // FIXME: refCount
    private final AtomicInteger usageCount = new AtomicInteger(1);

    private Runnable onCleanupCallback;

    /**
     * Default constructor. Resulting instance uses the given file threshold, and does not reset the data when the
     * {@link SizedStreamSource} returned by {@link #toStreamSource()} is finalized.
     *
     * @param config the configuration.
     */
    public SharedFileBackedOutputStream(final Configuration config) {
        super(config);
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
     * @param callback the callback
     */
    public void setOnCleanupCallback(final Runnable callback) {
        onCleanupCallback = requireNonNull(callback);
    }

    /**
     * Overridden to decrement the usage count.
     */
    // FIXME: decRef(), really
    @Override
    public void cleanup() {
        if (usageCount.get() <= 0) {
            throw new IllegalStateException("Usage count underflow");
        }

        if (usageCount.decrementAndGet() == 0) {
            super.cleanup();

            if (onCleanupCallback != null) {
                onCleanupCallback.run();
            }
        }
    }
}
