/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.routing;

import java.util.EventListener;
/**
 *
 * Listener which is interested in receiving RouteChangeEvents
 * for its local broker.
 * <p>
 * Listener is registerd via {@link RouteChangePublisher#registerRouteChangeListener(RouteChangeListener)}
 *
 *
 * @param <C> Type, which is used to represent Routing context.
 * @param <P> Type of data tree path, which is used to identify route.
 */
public interface RouteChangeListener<C,P> extends EventListener {

    /**
     * Callback which is invoked if there is an rpc routing table change.
     *
     * @param change Event representing change in local RPC routing table.
     */
    void onRouteChange(RouteChange<C, P> change);
}
