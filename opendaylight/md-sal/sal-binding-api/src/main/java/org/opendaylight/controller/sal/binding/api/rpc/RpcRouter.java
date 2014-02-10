/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.rpc;

import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangePublisher;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * RpcRouter is responsible for selecting RpcService based on provided routing
 * context identifier {@link RpcRoutingTable#getContextIdentifier()} and path in
 * overal data tree (@link {@link InstanceIdentifier}.
 *
 *
 * @author Tony Tkacik <ttkacik@cisco.com>
 *
 * @param <T>
 *            Type of RpcService for which router provides routing information
 *            and route selection.
 */
public interface RpcRouter<T extends RpcService> extends //
        RouteChangePublisher<Class<? extends BaseIdentity>, InstanceIdentifier<?>> {

    /**
     * Returns a type of RpcService which is served by this instance of router.
     *
     * @return type of RpcService which is served by this instance of router.
     */
    Class<T> getServiceType();


    /**
     * Returns a instance of T which is associated with this router instance
     * and routes messages based on routing tables.
     *
     * @return type of RpcService which is served by this instance of router.
     */
    T getInvocationProxy();

    /**
     * Returns a routing table for particular route context
     *
     * @param routeContext
     * @return Routing Table for particular route context.
     */
    <C extends BaseIdentity> RpcRoutingTable<C, T> getRoutingTable(Class<C> routeContext);

    /**
     * Returns an instance of RpcService which is responsible for processing
     * particular path.
     *
     * @param context
     *            Rpc Routing Context
     * @param path
     *            Instance Identifier which is used as a selector of instance.
     * @return instance of RpcService which is responsible for processing
     *         particular path.
     */
    T getService(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path);

    /**
     * Returns a default fallback instance of RpcService which is responsible
     * for handling all unknown imports.
     *
     * @return default instance responsible for processing RPCs.
     */
    T getDefaultService();

    Set<Class<? extends BaseIdentity>> getContexts();

    RoutedRpcRegistration<T> addRoutedRpcImplementation(T service);

    RpcRegistration<T> registerDefaultService(T service);

}
