/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.spi;

import java.util.Set;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.yang.common.QName;

public interface RpcRouter extends RpcProvisionRegistry, DOMRpcImplementation {

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation);

    @Override
    public RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation)
            throws IllegalArgumentException;

//    @Override
//    public Set<QName> getSupportedRpcs();
}
