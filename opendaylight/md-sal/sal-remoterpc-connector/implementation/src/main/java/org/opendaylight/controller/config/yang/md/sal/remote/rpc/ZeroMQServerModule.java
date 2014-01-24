/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.remote.rpc;

import org.opendaylight.controller.sal.connector.remoterpc.*;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.osgi.framework.BundleContext;

/**
*
*/
public final class ZeroMQServerModule extends org.opendaylight.controller.config.yang.md.sal.remote.rpc.AbstractZeroMQServerModule
 {

    private static final Integer ZEROMQ_ROUTER_PORT = 5554;
    private BundleContext bundleContext;

    public ZeroMQServerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ZeroMQServerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            ZeroMQServerModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        
        Broker broker = getDomBrokerDependency();

        final int port = getPort() != null ? getPort() : ZEROMQ_ROUTER_PORT;

        ServerImpl serverImpl = new ServerImpl(port);
        
        ClientImpl clientImpl = new ClientImpl();

    RoutingTableProvider provider = new RoutingTableProvider(bundleContext);//,serverImpl);


    facade.setRoutingTableProvider(provider );
    facade.setContext(bundleContext);
    facade.setRpcProvisionRegistry((RpcProvisionRegistry) broker);

    broker.registerProvider(facade, bundleContext);
    return facade;
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }
}
