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
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.osgi.framework.BundleContext;

public class DistributedOperationalDataStoreProviderModule
        extends AbstractDistributedOperationalDataStoreProviderModule {
    private BundleContext bundleContext;

    public DistributedOperationalDataStoreProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedOperationalDataStoreProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver,final DistributedOperationalDataStoreProviderModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(final AbstractDistributedOperationalDataStoreProviderModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // The DistributedOperDataStore is provided via blueprint so wait for and return it here for
        // backwards compatibility
        WaitingServiceTracker<DistributedDataStoreInterface> tracker = WaitingServiceTracker.create(
                DistributedDataStoreInterface.class, bundleContext, "(type=distributed-operational)");
        DistributedDataStoreInterface delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new ForwardingDistributedDataStore(delegate, tracker);
    }

    public static DatastoreContext newDatastoreContext() {
        return newDatastoreContext(null);
    }

    private static DatastoreContext newDatastoreContext(final OperationalProperties inProps) {
        OperationalProperties props = inProps;
        if (props == null) {
            props = new OperationalProperties();
        }

        return DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.OPERATIONAL)
                .tempFileDirectory("./data")
                .fileBackedStreamingThresholdInMegabytes(props.getFileBackedStreamingThresholdInMegabytes()
                        .getValue().intValue())
                .maxShardDataChangeExecutorPoolSize(props.getMaxShardDataChangeExecutorPoolSize().getValue().intValue())
                .maxShardDataChangeExecutorQueueSize(props.getMaxShardDataChangeExecutorQueueSize()
                        .getValue().intValue())
                .maxShardDataChangeListenerQueueSize(props.getMaxShardDataChangeListenerQueueSize()
                        .getValue().intValue())
                .maxShardDataStoreExecutorQueueSize(props.getMaxShardDataStoreExecutorQueueSize().getValue().intValue())
                .shardTransactionIdleTimeoutInMinutes(props.getShardTransactionIdleTimeoutInMinutes().getValue())
                .operationTimeoutInSeconds(props.getOperationTimeoutInSeconds().getValue())
                .shardJournalRecoveryLogBatchSize(props.getShardJournalRecoveryLogBatchSize()
                        .getValue().intValue())
                .shardSnapshotBatchCount(props.getShardSnapshotBatchCount().getValue().intValue())
                .shardSnapshotDataThresholdPercentage(props.getShardSnapshotDataThresholdPercentage()
                        .getValue().intValue())
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
                .useTellBasedProtocol(props.getUseTellBasedProtocol())
                .syncIndexThreshold(props.getSyncIndexThreshold().getValue())
                .build();
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
