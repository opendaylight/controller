package org.opendaylight.controller.config.yang.config.sal_clustering_service_provider;

import org.opendaylight.controller.md.sal.clustering.service.ClusteringServiceFactory;
import org.opendaylight.controller.md.sal.clustering.service.listener.LastRoleChangeListener;

public class SalClusteringServiceProviderModule extends org.opendaylight.controller.config.yang.config.sal_clustering_service_provider.AbstractSalClusteringServiceProviderModule {
    public SalClusteringServiceProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SalClusteringServiceProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.sal_clustering_service_provider.SalClusteringServiceProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LastRoleChangeListener lastRoleChangeListener = new LastRoleChangeListener();
        getNotificationServiceDependency().registerNotificationListener(lastRoleChangeListener);
        return ClusteringServiceFactory.getFactory().getClusteringServiceInstance(
                getNotificationServiceDependency(), lastRoleChangeListener);
    }

}
