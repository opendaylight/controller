package org.opendaylight.controller.config.yang.config.netconf.northbound.impl;

import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;

public class NetconfServerMonitoringModule extends org.opendaylight.controller.config.yang.config.netconf.northbound.impl.AbstractNetconfServerMonitoringModule {
    public NetconfServerMonitoringModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfServerMonitoringModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.netconf.northbound.impl.NetconfServerMonitoringModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new NetconfMonitoringServiceImpl(getAggregatorDependency());
    }

}
