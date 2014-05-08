/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.routing;

import java.util.Map;
import java.util.Set;
/**
 * Event representing change in RPC routing table.
 *
 *
 * @param <C> Type, which is used to represent Routing context.
 * @param <P> Type of data tree path, which is used to identify route.
 */
public interface RouteChange<C,P> {

    /**
     *
     * Returns a map of removed routes in associated routing contexts.
     * <p>
     * This map represents routes, which were withdrawn from broker local
     * routing table and broker may need to forward RPC to other broker
     * in order to process RPC request.
     *
     * @return Map of contexts and removed routes
     */
    Map<C,Set<P>> getRemovals();
    /**
    *
    * Returns a map of announced routes in associated routing contexts.
    *
    * This map represents routes, which were announced by broker
    * and are present in broker's local routing table. This routes
    * are processed by implementations which are registered
    * to originating broker.
    *
    * @return Map of contexts and announced routes
    */
    Map<C,Set<P>> getAnnouncements();
}
