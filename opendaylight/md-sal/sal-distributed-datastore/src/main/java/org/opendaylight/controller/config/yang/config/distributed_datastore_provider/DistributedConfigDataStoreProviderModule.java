/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.osgi.framework.BundleContext;

public class DistributedConfigDataStoreProviderModule extends
    org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedConfigDataStoreProviderModule {
    private BundleContext bundleContext;

    public DistributedConfigDataStoreProviderModule(
        org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedConfigDataStoreProviderModule(
        org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
        org.opendaylight.controller.config.yang.config.distributed_datastore_provider.DistributedConfigDataStoreProviderModule oldModule,
        java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractDistributedConfigDataStoreProviderModule oldModule) {
        return true;
    }

    @Override
    public AutoCloseable createInstance() {
        // The DistributedConfigDataStore is provided via blueprint so wait for and return it here for
        // backwards compatibility.
        WaitingServiceTracker<DistributedDataStoreInterface> tracker = WaitingServiceTracker.create(
                DistributedDataStoreInterface.class, bundleContext, "(type=distributed-config)");
        DistributedDataStoreInterface delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new ForwardingDistributedDataStore(delegate, tracker);
    }

    public static DatastoreContext newDatastoreContext() {
        return newDatastoreContext(null);
    }

    private static DatastoreContext newDatastoreContext(ConfigProperties inProps) {
        ConfigProperties props = inProps;
        if(props == null) {
            props = new ConfigProperties();
        }

        return DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .maxShardDataChangeExecutorPoolSize(props.getMaxShardDataChangeExecutorPoolSize().getValue().intValue())
                .maxShardDataChangeExecutorQueueSize(props.getMaxShardDataChangeExecutorQueueSize().getValue().intValue())
                .maxShardDataChangeListenerQueueSize(props.getMaxShardDataChangeListenerQueueSize().getValue().intValue())
                .maxShardDataStoreExecutorQueueSize(props.getMaxShardDataStoreExecutorQueueSize().getValue().intValue())
                .shardTransactionIdleTimeoutInMinutes(props.getShardTransactionIdleTimeoutInMinutes().getValue())
                .operationTimeoutInSeconds(props.getOperationTimeoutInSeconds().getValue())
                .shardJournalRecoveryLogBatchSize(props.getShardJournalRecoveryLogBatchSize().
                        getValue().intValue())
                .shardSnapshotBatchCount(props.getShardSnapshotBatchCount().getValue().intValue())
                .shardSnapshotDataThresholdPercentage(props.getShardSnapshotDataThresholdPercentage().getValue().intValue())
                .shardHeartbeatIntervalInMillis(props.getShardHeartbeatIntervalInMillis().getValue())
                .shardInitializationTimeoutInSeconds(props.getShardInitializationTimeoutInSeconds().getValue())
                .shardLeaderElectionTimeoutInSeconds(props.getShardLeaderElectionTimeoutInSeconds().getValue())
                .shardTransactionCommitTimeoutInSeconds(
                        props.getShardTransactionCommitTimeoutInSeconds().getValue().intValue())
                .shardTransactionCommitQueueCapacity(
                        props.getShardTransactionCommitQueueCapacity().getValue().intValue())
                .persistent(props.getPersistent().booleanValue())
                .shardIsolatedLeaderCheckIntervalInMillis(
                    props.getShardIsolatedLeaderCheckIntervalInMillis().getValue())
                .shardElectionTimeoutFactor(props.getShardElectionTimeoutFactor().getValue())
                .transactionCreationInitialRateLimit(props.getTransactionCreationInitialRateLimit().getValue())
                .shardBatchedModificationCount(props.getShardBatchedModificationCount().getValue().intValue())
                .shardCommitQueueExpiryTimeoutInSeconds(
                        props.getShardCommitQueueExpiryTimeoutInSeconds().getValue().intValue())
                .transactionDebugContextEnabled(props.getTransactionDebugContextEnabled())
                .customRaftPolicyImplementation(props.getCustomRaftPolicyImplementation())
                .shardSnapshotChunkSize(props.getShardSnapshotChunkSize().getValue().intValue())
                .build();
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
