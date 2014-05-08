/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

class RoutedRpcRegImpl extends AbstractObjectRegistration<RpcImplementation> implements
        RoutedRpcRegistration {

    private final QName type;
    private final RoutedRpcSelector router;

    public RoutedRpcRegImpl(final QName rpcType, final RpcImplementation implementation, final RoutedRpcSelector routedRpcSelector) {
        super(implementation);
        this.type = rpcType;
        router = routedRpcSelector;
    }

    @Override
    public void registerPath(final QName context, final InstanceIdentifier path) {
        router.addPath(context, path, this);
    }

    @Override
    public void unregisterPath(final QName context, final InstanceIdentifier path) {
        router.removePath(context, path, this);
    }

    @Override
    protected void removeRegistration() {

    }

    @Override
    public QName getType() {
        return type;
    }

}