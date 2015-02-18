package org.opendaylight.controller.config.yang.config.netconf.northbound.impl;

import org.opendaylight.controller.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory;

public class NetconfMapperAggregatorModule extends org.opendaylight.controller.config.yang.config.netconf.northbound.impl.AbstractNetconfMapperAggregatorModule {
    public NetconfMapperAggregatorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfMapperAggregatorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.config.netconf.northbound.impl.NetconfMapperAggregatorModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {}

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new AggregatedNetconfOperationServiceFactory();
    }

}
