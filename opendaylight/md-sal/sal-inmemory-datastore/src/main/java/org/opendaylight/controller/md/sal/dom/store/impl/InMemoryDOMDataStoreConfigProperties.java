/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl;

/**
 * Holds configuration properties when creating an {@link InMemoryDOMDataStore} instance via the
 * {@link InMemoryDOMDataStoreFactory}
 *
 * @author Thomas Pantelis
 * @see InMemoryDOMDataStoreFactory
 */
public class InMemoryDOMDataStoreConfigProperties {

    public static final int DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE = 1000;
    public static final int DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE = 20;
    public static final int DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE = 1000;
    public static final int DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE = 5000;

    private static final InMemoryDOMDataStoreConfigProperties DEFAULT =
            create(DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE,
                    DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE,
                    DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE,
                    DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE);

    private final int maxDataChangeExecutorQueueSize;
    private final int maxDataChangeExecutorPoolSize;
    private final int maxDataChangeListenerQueueSize;
    private final int maxDataStoreExecutorQueueSize;

    /**
     * Constructs an instance with the given property values.
     *
     * @param maxDataChangeExecutorPoolSize
     *            maximum thread pool size for the data change notification executor.
     * @param maxDataChangeExecutorQueueSize
     *            maximum queue size for the data change notification executor.
     * @param maxDataChangeListenerQueueSize
     *            maximum queue size for the data change listeners.
     * @param maxDataStoreExecutorQueueSize
     *            maximum queue size for the data store executor.
     */
    public static InMemoryDOMDataStoreConfigProperties create(int maxDataChangeExecutorPoolSize,
            int maxDataChangeExecutorQueueSize, int maxDataChangeListenerQueueSize,
            int maxDataStoreExecutorQueueSize) {
        return new InMemoryDOMDataStoreConfigProperties(maxDataChangeExecutorPoolSize,
                maxDataChangeExecutorQueueSize, maxDataChangeListenerQueueSize,
                maxDataStoreExecutorQueueSize);
    }

    public static InMemoryDOMDataStoreConfigProperties create(int maxDataChangeExecutorPoolSize,
            int maxDataChangeExecutorQueueSize, int maxDataChangeListenerQueueSize) {
        return new InMemoryDOMDataStoreConfigProperties(maxDataChangeExecutorPoolSize,
                maxDataChangeExecutorQueueSize, maxDataChangeListenerQueueSize,
                DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE);
    }

    /**
     * Returns the InMemoryDOMDataStoreConfigProperties instance with default values.
     */
    public static InMemoryDOMDataStoreConfigProperties getDefault() {
        return DEFAULT;
    }

    private InMemoryDOMDataStoreConfigProperties(int maxDataChangeExecutorPoolSize,
            int maxDataChangeExecutorQueueSize, int maxDataChangeListenerQueueSize,
            int maxDataStoreExecutorQueueSize) {
        this.maxDataChangeExecutorQueueSize = maxDataChangeExecutorQueueSize;
        this.maxDataChangeExecutorPoolSize = maxDataChangeExecutorPoolSize;
        this.maxDataChangeListenerQueueSize = maxDataChangeListenerQueueSize;
        this.maxDataStoreExecutorQueueSize = maxDataStoreExecutorQueueSize;
    }

    /**
     * Returns the maximum queue size for the data change notification executor.
     */
    public int getMaxDataChangeExecutorQueueSize() {
        return maxDataChangeExecutorQueueSize;
    }

    /**
     * Returns the maximum thread pool size for the data change notification executor.
     */
    public int getMaxDataChangeExecutorPoolSize() {
        return maxDataChangeExecutorPoolSize;
    }

    /**
     * Returns the maximum queue size for the data change listeners.
     */
    public int getMaxDataChangeListenerQueueSize() {
        return maxDataChangeListenerQueueSize;
    }

    /**
     * Returns the maximum queue size for the data store executor.
     */
    public int getMaxDataStoreExecutorQueueSize() {
        return maxDataStoreExecutorQueueSize;
    }
}
