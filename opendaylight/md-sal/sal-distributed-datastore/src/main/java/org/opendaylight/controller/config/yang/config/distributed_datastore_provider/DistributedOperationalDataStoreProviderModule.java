/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreSnapshotRestore;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreFactory;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.osgi.framework.BundleContext;

public class DistributedOperationalDataStoreProviderModule extends
    org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedOperationalDataStoreProviderModule {
    private BundleContext bundleContext;

    public DistributedOperationalDataStoreProviderModule(
        org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedOperationalDataStoreProviderModule(
        org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
        org.opendaylight.controller.config.yang.config.distributed_datastore_provider.DistributedOperationalDataStoreProviderModule oldModule,
        java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractDistributedOperationalDataStoreProviderModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        OperationalProperties props = getOperationalProperties();
        if(props == null) {
            props = new OperationalProperties();
        }

        DatastoreContext datastoreContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.OPERATIONAL)
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

        return DistributedDataStoreFactory.createInstance(getOperationalSchemaServiceDependency(),
                datastoreContext, DatastoreSnapshotRestore.instance().getAndRemove(datastoreContext.getDataStoreName()),
                getOperationalActorSystemProviderDependency().getActorSystem(), bundleContext);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
