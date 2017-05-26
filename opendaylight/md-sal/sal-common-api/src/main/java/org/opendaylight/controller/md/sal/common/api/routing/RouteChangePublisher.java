/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.routing;

import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Publishes changes in local RPC routing table to registered listener.
 *
 * @param <C> Type, which is used to represent Routing context.
 * @param <P> Type of data tree path, which is used to identify route.
 */
public interface RouteChangePublisher<C,P> {

    <L extends RouteChangeListener<C,P>> ListenerRegistration<L> registerRouteChangeListener(L listener);
}
