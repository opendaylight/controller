package org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.xsql.XSQLProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class XSQLModule extends org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.AbstractXSQLModule {
    public static XSQLAdapter xsqlAdapter;

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
        xsqlAdapter = new XSQLAdapter();
        final ListenerRegistration<SchemaContextListener> schemaListenerReg = getSchemaServiceDependency()
                .registerSchemaContextListener(xsqlAdapter);
        xsqlAdapter.setDataBroker(getAsyncDataBrokerDependency());
        XSQLProvider provider = new XSQLProvider();
        provider.buildXSQL(getDataBrokerDependency());
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                schemaListenerReg.close();
                xsqlAdapter.close();
            }
        };
    }

    public static XSQLAdapter getXSQLAdapter() {
        return xsqlAdapter;
    }
}
