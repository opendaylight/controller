/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.io;

import com.google.common.base.Preconditions;
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
public class SharedFileBackedOutputStream extends FileBackedOutputStream {
    private final AtomicInteger usageCount = new AtomicInteger(1);
    @SuppressWarnings("rawtypes")
    private Consumer onCleanupCallback;
    private Object onCleanupContext;

    public SharedFileBackedOutputStream(final int fileThreshold, final String fileDirectory) {
        super(fileThreshold, fileDirectory);
    }

    public SharedFileBackedOutputStream(final int fileThreshold, final Path fileDirectory) {
        super(fileThreshold, fileDirectory);
    }

    /**
     * Increments the usage count. This must be followed by a corresponding call to {@link #cleanup()} when this
     * instance is no longer needed.
     */
    public void incrementUsageCount() {
        usageCount.getAndIncrement();
    }

    /**
     * Returns the current usage count.
     *
     * @return the current usage count
     */
    public int getUsageCount() {
        return usageCount.get();
    }

    /**
     * Sets the callback to be notified when {@link FileBackedOutputStream#cleanup()} is called to delete the backing
     * file.
     */
    public <T> void setOnCleanupCallback(final Consumer<T> callback, final T context) {
        onCleanupCallback = callback;
        onCleanupContext = context;
    }

    /**
     * Overridden to decrement the usage count.
     */
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
