/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Interface defining provider's access to the Rpc Registry which could be used
 * to register their implementations of service to the MD-SAL.
 *
 * @author ttkacik
 *
 */
public interface RpcProviderRegistry extends //
        RpcConsumerRegistry, //
        RouteChangePublisher<RpcContextIdentifier, InstanceIdentifier<?>> {
    /**
     * Registers an global RpcService implementation.
     *
     * @param type
     * @param implementation
     * @return
     */
    <T extends RpcService> RpcRegistration<T> addRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException;

    /**
     *
     * Register an Routed RpcService where routing is determined on annotated
     * (in YANG model) context-reference and value of annotated leaf.
     *
     * @param type
     *            Type of RpcService, use generated interface class, not your
     *            implementation clas
     * @param implementation
     *            Implementation of RpcService
     * @return Registration object for routed Rpc which could be used to close
     *         an
     *
     * @throws IllegalStateException
     */
    <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> type, T implementation)
            throws IllegalStateException;
}
