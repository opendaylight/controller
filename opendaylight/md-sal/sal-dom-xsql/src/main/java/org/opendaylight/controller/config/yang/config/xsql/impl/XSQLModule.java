package org.opendaylight.controller.config.yang.config.xsql.impl;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.xsql.XSQLImpl;

public class XSQLModule extends org.opendaylight.controller.config.yang.config.xsql.impl.AbstractXSQLModule {
    public XSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public XSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.xsql.impl.XSQLModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        XSQLAdapter xsqlAdapter = XSQLAdapter.getInstance();
        getSchemaServiceDependency().registerSchemaServiceListener(xsqlAdapter);
        xsqlAdapter.setDataBroker(getAsyncDataBrokerDependency());
    	return new XSQLImpl();
    }

}
