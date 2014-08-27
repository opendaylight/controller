package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;

import scala.concurrent.duration.Duration;

public class DistributedConfigDataStoreProviderModule extends
    org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedConfigDataStoreProviderModule {
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
    public java.lang.AutoCloseable createInstance() {

        ConfigProperties props = getConfigProperties();
        if(props == null) {
            props = new ConfigProperties();
        }

        DatastoreContext datastoreContext = new DatastoreContext("DistributedConfigDatastore",
                InMemoryDOMDataStoreConfigProperties.create(
                        props.getMaxShardDataChangeExecutorPoolSize(),
                        props.getMaxShardDataChangeExecutorQueueSize(),
                        props.getMaxShardDataChangeListenerQueueSize(),
                        props.getMaxShardDataStoreExecutorQueueSize()),
                Duration.create(props.getShardTransactionIdleTimeoutInMinutes(),
                        TimeUnit.MINUTES));

        return DistributedDataStoreFactory.createInstance("config", getConfigSchemaServiceDependency(),
                datastoreContext);
    }
}
