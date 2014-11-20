package org.opendaylight.controller.config.yang.config.topology_manager;

import org.opendaylight.md.controller.topology.manager.FlowCapableTopologyProvider;

public class FlowCapableTopologyProviderModule extends org.opendaylight.controller.config.yang.config.topology_manager.AbstractFlowCapableTopologyProviderModule {
    public FlowCapableTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FlowCapableTopologyProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.topology_manager.FlowCapableTopologyProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        FlowCapableTopologyProvider flowCapableTopologyProvider = new FlowCapableTopologyProvider(getDataBrokerDependency(), getNotificationServiceDependency());
        flowCapableTopologyProvider.intialize();
        return flowCapableTopologyProvider;
    }

}
