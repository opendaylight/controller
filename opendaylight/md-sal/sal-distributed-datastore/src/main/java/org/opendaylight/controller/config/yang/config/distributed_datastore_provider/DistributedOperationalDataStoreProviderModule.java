package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.osgi.framework.BundleContext;

import scala.concurrent.duration.Duration;

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
    public java.lang.AutoCloseable createInstance() {

        OperationalProperties props = getOperationalProperties();
        if(props == null) {
            props = new OperationalProperties();
        }

        DatastoreContext datastoreContext = DatastoreContext.newBuilder()
                .dataStoreType("operational")
                .dataStoreProperties(InMemoryDOMDataStoreConfigProperties.create(
                        props.getMaxShardDataChangeExecutorPoolSize().getValue().intValue(),
                        props.getMaxShardDataChangeExecutorQueueSize().getValue().intValue(),
                        props.getMaxShardDataChangeListenerQueueSize().getValue().intValue(),
                        props.getMaxShardDataStoreExecutorQueueSize().getValue().intValue()))
                .shardTransactionIdleTimeout(Duration.create(
                        props.getShardTransactionIdleTimeoutInMinutes().getValue(), TimeUnit.MINUTES))
                .operationTimeoutInSeconds(props.getOperationTimeoutInSeconds().getValue())
                .shardJournalRecoveryLogBatchSize(props.getShardJournalRecoveryLogBatchSize().
                        getValue().intValue())
                .shardSnapshotBatchCount(props.getShardSnapshotBatchCount().getValue().intValue())
                .shardSnapshotDataThresholdPercentage(props.getShardSnapshotDataThresholdPercentage().getValue().intValue())
                .shardHeartbeatIntervalInMillis(props.getShardHearbeatIntervalInMillis().getValue())
                .shardInitializationTimeout(props.getShardInitializationTimeoutInSeconds().getValue(),
                        TimeUnit.SECONDS)
                .shardLeaderElectionTimeout(props.getShardLeaderElectionTimeoutInSeconds().getValue(),
                        TimeUnit.SECONDS)
                .shardTransactionCommitTimeoutInSeconds(
                        props.getShardTransactionCommitTimeoutInSeconds().getValue().intValue())
                .shardTransactionCommitQueueCapacity(
                        props.getShardTransactionCommitQueueCapacity().getValue().intValue())
                .persistent(props.getPersistent().booleanValue())
                .shardIsolatedLeaderCheckIntervalInMillis(
                    props.getShardIsolatedLeaderCheckIntervalInMillis().getValue())
                .shardElectionTimeoutFactor(props.getShardElectionTimeoutFactor().getValue())
                .transactionCreationInitialRateLimit(props.getTxCreationInitialRateLimit().getValue())
                .build();

        return DistributedDataStoreFactory.createInstance(getOperationalSchemaServiceDependency(),
                datastoreContext, bundleContext);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
