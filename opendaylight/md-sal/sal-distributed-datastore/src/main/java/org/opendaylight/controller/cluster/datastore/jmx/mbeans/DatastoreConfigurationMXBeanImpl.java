/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

/**
 * Implementation of DatastoreConfigurationMXBean.
 *
 * @author Thomas Pantelis
 */
public class DatastoreConfigurationMXBeanImpl extends AbstractMXBean implements DatastoreConfigurationMXBean {
    public static final String JMX_CATEGORY_CONFIGURATION = "Configuration";

    private DatastoreContext context;

    public DatastoreConfigurationMXBeanImpl(String mxBeanType) {
        super("Datastore", mxBeanType, JMX_CATEGORY_CONFIGURATION);
    }

    public void setContext(DatastoreContext context) {
        this.context = context;
    }

    @Override
    public long getShardTransactionIdleTimeoutInSeconds() {
        return context.getShardTransactionIdleTimeout().toSeconds();
    }

    @Override
    public long getOperationTimeoutInSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(context.getOperationTimeoutInMillis());
    }

    @Override
    public long getShardHeartbeatIntervalInMillis() {
        return context.getShardRaftConfig().getHeartBeatInterval().toMillis();
    }

    @Override
    public int getShardJournalRecoveryLogBatchSize() {
        return context.getShardRaftConfig().getJournalRecoveryLogBatchSize();
    }

    @Override
    public long getShardIsolatedLeaderCheckIntervalInMillis() {
        return context.getShardRaftConfig().getIsolatedCheckIntervalInMillis();
    }

    @Override
    public long getShardElectionTimeoutFactor() {
        return context.getShardRaftConfig().getElectionTimeoutFactor();
    }

    @Override
    public int getShardSnapshotDataThresholdPercentage() {
        return context.getShardRaftConfig().getSnapshotDataThresholdPercentage();
    }

    @Override
    public long getShardSnapshotBatchCount() {
        return context.getShardRaftConfig().getSnapshotBatchCount();
    }

    @Override
    public long getShardTransactionCommitTimeoutInSeconds() {
        return context.getShardTransactionCommitTimeoutInSeconds();
    }

    @Override
    public long getShardCommitQueueExpiryTimeoutInSeconds() {
        return TimeUnit.SECONDS.convert(context.getShardCommitQueueExpiryTimeoutInMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int getShardTransactionCommitQueueCapacity() {
        return context.getShardTransactionCommitQueueCapacity();
    }

    @Override
    public long getShardInitializationTimeoutInSeconds() {
        return context.getShardInitializationTimeout().duration().toSeconds();
    }

    @Override
    public long getShardLeaderElectionTimeoutInSeconds() {
        return context.getShardLeaderElectionTimeout().duration().toSeconds();
    }

    @Override
    public boolean isPersistent() {
        return context.isPersistent();
    }

    @Override
    public long getTransactionCreationInitialRateLimit() {
        return context.getTransactionCreationInitialRateLimit();
    }

    @Override
    public boolean getTransactionContextDebugEnabled() {
        return context.isTransactionDebugContextEnabled();
    }

    @Override
    public int getMaxShardDataChangeExecutorPoolSize() {
        return context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize();
    }

    @Override
    public int getMaxShardDataChangeExecutorQueueSize() {
        return context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize();
    }

    @Override
    public int getMaxShardDataChangeListenerQueueSize() {
        return context.getDataStoreProperties().getMaxDataChangeListenerQueueSize();
    }

    @Override
    public int getMaxShardDataStoreExecutorQueueSize() {
        return context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize();
    }

    @Override
    public int getMaximumMessageSliceSize() {
        return context.getMaximumMessageSliceSize();
    }

    @Override
    public int getPersistentActorRestartMinBackoffInSeconds() {
        return context.getPersistentActorRestartMinBackoffInSeconds();
    }

    @Override
    public int getPersistentActorRestartMaxBackoffInSeconds() {
        return context.getPersistentActorRestartMaxBackoffInSeconds();
    }

    @Override
    public int getPersistentActorRestartResetBackoffInSeconds() {
        return context.getPersistentActorRestartResetBackoffInSeconds();
    }
}
