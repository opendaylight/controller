package org.opendaylight.controller.config.yang.config.cluster_config_provider;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.config.ClusterConfigRpcService;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;

public class ClusterConfigProviderModule extends AbstractClusterConfigProviderModule {
    public ClusterConfigProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ClusterConfigProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver,
            ClusterConfigProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        Preconditions.checkArgument(getConfigDataStoreDependency() instanceof DistributedDataStore,
                "Injected config DOMStore must be an instance of DistributedDataStore");
        Preconditions.checkArgument(getOperDataStoreDependency() instanceof DistributedDataStore,
                "Injected operational DOMStore must be an instance of DistributedDataStore");
        ClusterConfigRpcService service = new ClusterConfigRpcService((DistributedDataStore)getConfigDataStoreDependency(),
                (DistributedDataStore)getOperDataStoreDependency());
        service.start(getRpcRegistryDependency());
        return service;
    }
}
