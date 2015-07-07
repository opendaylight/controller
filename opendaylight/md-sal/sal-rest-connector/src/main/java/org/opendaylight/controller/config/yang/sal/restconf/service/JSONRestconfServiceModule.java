package org.opendaylight.controller.config.yang.sal.restconf.service;

import org.opendaylight.controller.sal.restconf.impl.JSONRestconfServiceImpl;

public class JSONRestconfServiceModule extends org.opendaylight.controller.config.yang.sal.restconf.service.AbstractJSONRestconfServiceModule {
    public JSONRestconfServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public JSONRestconfServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.sal.restconf.service.JSONRestconfServiceModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new JSONRestconfServiceImpl();
    }
}
