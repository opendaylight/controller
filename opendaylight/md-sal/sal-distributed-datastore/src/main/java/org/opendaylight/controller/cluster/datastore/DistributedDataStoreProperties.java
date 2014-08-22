/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

/**
 * Wrapper class for DistributedDataStore configuration properties.
 *
 * @author Thomas Pantelis
 */
public class DistributedDataStoreProperties {
    private final int maxShardDataChangeListenerQueueSize;
    private final int maxShardDataChangeExecutorQueueSize;
    private final int maxShardDataChangeExecutorPoolSize;
    private final int shardTransactionIdleTimeoutInMinutes;

    public DistributedDataStoreProperties() {
        maxShardDataChangeListenerQueueSize = 1000;
        maxShardDataChangeExecutorQueueSize = 1000;
        maxShardDataChangeExecutorPoolSize = 20;
        shardTransactionIdleTimeoutInMinutes = 10;
    }

    public DistributedDataStoreProperties(int maxShardDataChangeListenerQueueSize,
            int maxShardDataChangeExecutorQueueSize, int maxShardDataChangeExecutorPoolSize,
            int shardTransactionIdleTimeoutInMinutes) {
        this.maxShardDataChangeListenerQueueSize = maxShardDataChangeListenerQueueSize;
        this.maxShardDataChangeExecutorQueueSize = maxShardDataChangeExecutorQueueSize;
        this.maxShardDataChangeExecutorPoolSize = maxShardDataChangeExecutorPoolSize;
        this.shardTransactionIdleTimeoutInMinutes = shardTransactionIdleTimeoutInMinutes;
    }

    public int getMaxShardDataChangeListenerQueueSize() {
        return maxShardDataChangeListenerQueueSize;
    }

    public int getMaxShardDataChangeExecutorQueueSize() {
        return maxShardDataChangeExecutorQueueSize;
    }

    public int getMaxShardDataChangeExecutorPoolSize() {
        return maxShardDataChangeExecutorPoolSize;
    }

    public int getShardTransactionIdleTimeoutInMinutes() {
        return shardTransactionIdleTimeoutInMinutes;
    }
}
