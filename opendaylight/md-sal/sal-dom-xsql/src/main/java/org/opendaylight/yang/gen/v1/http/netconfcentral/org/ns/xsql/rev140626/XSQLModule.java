package org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.xsql.XSQLProvider;

public class XSQLModule extends org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.AbstractXSQLModule {
    public XSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public XSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        XSQLAdapter xsqlAdapter = XSQLAdapter.getInstance();
        getSchemaServiceDependency().registerSchemaContextListener(xsqlAdapter);
        xsqlAdapter.setDataBroker(getAsyncDataBrokerDependency());
        final XSQLProvider p = new XSQLProvider();
        Runnable runthis = new Runnable() {
            @Override
            public void run() {
                try{Thread.sleep(10000);}catch(Exception err){}
                p.buildXSQL(getDataBrokerDependency());
            }
        };
        return p;
    }
}
