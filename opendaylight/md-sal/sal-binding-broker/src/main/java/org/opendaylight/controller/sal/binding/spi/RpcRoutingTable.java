/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.routing.MutableRoutingTable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public interface RpcRoutingTable<C extends BaseIdentity, S extends RpcService> extends
        MutableRoutingTable<Class<? extends C>, InstanceIdentifier<?>, S> {

    Class<C> getIdentifier();

    /**
     * Updates route for particular path to specified instance of
     * {@link RpcService}.
     * 
     * @param path
     *            Path for which RpcService routing is to be updated
     * @param service
     *            Instance of RpcService which is responsible for processing Rpc
     *            Requests.
     */
    void updateRoute(InstanceIdentifier<?> path, S service);

    /**
     * Deletes a route for particular path
     * 
     * @param path
     *            Path for which
     */
    void removeRoute(InstanceIdentifier<?> path);

    /**
     * 
     */
    S getRoute(InstanceIdentifier<?> nodeInstance);

    /**
     * 
     * @return
     */
    Map<InstanceIdentifier<?>, S> getRoutes();
}
