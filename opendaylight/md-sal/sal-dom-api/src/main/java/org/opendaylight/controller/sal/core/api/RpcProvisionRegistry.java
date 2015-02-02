/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @deprecated Use {@link org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService} and {@link org.opendaylight.controller.md.sal.dom.api.DOMRpcService} instead.
 */
@Deprecated
public interface RpcProvisionRegistry extends RpcImplementation, BrokerService, RouteChangePublisher<RpcRoutingContext, YangInstanceIdentifier>, DOMService {

    /**
     * Registers an implementation of the rpc.
     *
     * <p>
     * The registered rpc functionality will be available to all other
     * consumers and providers registered to the broker, which are aware of
     * the {@link QName} assigned to the rpc.
     *
     * <p>
     * There is no assumption that rpc type is in the set returned by
     * invoking {@link RpcImplementation#getSupportedRpcs()}. This allows
     * for dynamic rpc implementations.
     *
     * @param rpcType
     *            Name of Rpc
     * @param implementation
     *            Provider's Implementation of the RPC functionality
     * @throws IllegalArgumentException
     *             If the name of RPC is invalid
     */
    RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation)
            throws IllegalArgumentException;

    ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(RpcRegistrationListener listener);

    RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation);

  /**
   * Sets this RoutedRpc Implementation as a delegate rpc provider and will be asked to invoke rpc if the
   * current provider can't service the rpc request
   *
   * @param defaultImplementation
   *              Provider's implementation of RPC functionality
   */
    public void setRoutedRpcDefaultDelegate(RoutedRpcDefaultImplementation defaultImplementation);
}
