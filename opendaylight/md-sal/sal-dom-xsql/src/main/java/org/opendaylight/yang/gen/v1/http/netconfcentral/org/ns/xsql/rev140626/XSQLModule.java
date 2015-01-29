/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.xsql.XSQLProvider;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class XSQLModule extends org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.AbstractXSQLModule {
    private static final long SLEEP_TIME_BEFORE_CREATING_TRANSACTION = 10000;
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
                try{Thread.sleep(SLEEP_TIME_BEFORE_CREATING_TRANSACTION);}catch(Exception err){}
                p.buildXSQL(getDataBrokerDependency());
            }
        };
        return p;
    }
}
