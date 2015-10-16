/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016;

import org.opendaylight.controller.md.sal.dom.odl.sql.ODLSQLServiceImpl;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ODLSQLModule
        extends org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.AbstractODLSQLModule {
    public ODLSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ODLSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.ODLSQLModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final RpcRegistration<OdlSqlService> service = getRpcRegistryDependency()
                .addRpcImplementation(OdlSqlService.class, new ODLSQLServiceImpl(getSchemaServiceDependency()));
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                service.close();
            }
        };
    }
}
