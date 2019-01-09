/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;


/**
 * MXBean interface for data store configuration.
 *
 * @author Thomas Pantelis
 */
public interface DatastoreConfigurationMXBean {
    long getShardTransactionIdleTimeoutInSeconds();

    long getOperationTimeoutInSeconds();

    long getShardHeartbeatIntervalInMillis();

    int getShardJournalRecoveryLogBatchSize();

    long getShardIsolatedLeaderCheckIntervalInMillis();

    long getShardElectionTimeoutFactor();

    int getShardSnapshotDataThresholdPercentage();

    long getShardSnapshotBatchCount();

    long getShardTransactionCommitTimeoutInSeconds();

    int getShardTransactionCommitQueueCapacity();

    long getShardCommitQueueExpiryTimeoutInSeconds();

    long getShardInitializationTimeoutInSeconds();

    long getShardLeaderElectionTimeoutInSeconds();

    boolean isPersistent();

    long getTransactionCreationInitialRateLimit();

    boolean getTransactionContextDebugEnabled();

    int getMaxShardDataChangeExecutorPoolSize();

    int getMaxShardDataChangeExecutorQueueSize();

    int getMaxShardDataChangeListenerQueueSize();

    int getMaxShardDataStoreExecutorQueueSize();

    int getMaximumMessageSliceSize();

    int getPersistentActorRestartMinBackoffInSeconds();

    int getPersistentActorRestartMaxBackoffInSeconds();

    int getPersistentActorRestartResetBackoffInSeconds();
}
