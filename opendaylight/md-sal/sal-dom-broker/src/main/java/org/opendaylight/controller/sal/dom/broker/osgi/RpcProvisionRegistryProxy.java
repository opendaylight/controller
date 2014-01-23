/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class RpcProvisionRegistryProxy extends AbstractBrokerServiceProxy<RpcProvisionRegistry>
                                       implements RpcProvisionRegistry {

    public RpcProvisionRegistryProxy(ServiceReference<RpcProvisionRegistry> ref, RpcProvisionRegistry delegate) {
        super(ref, delegate);
    }

    @Override
    public Broker.RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation) throws IllegalArgumentException {
        return getDelegate().addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(RpcRegistrationListener listener) {
        return getDelegate().addRpcRegistrationListener(listener);
    }

    @Override
    public Broker.RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        return getDelegate().addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(L listener) {
        return getDelegate().registerRouteChangeListener(listener);
    }
}
