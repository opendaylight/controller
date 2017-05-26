/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.routing;

import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Base interface for a routed RPC RPC implementation registration.
 *
 * @param <C> the context type used for routing
 * @param <P> the path identifier type
 * @param <S> the RPC implementation type
 */
public interface RoutedRegistration<C, P extends Path<P>, S> extends Registration {

    /**
     * Registers the RPC implementation associated with this registration for the given path
     * identifier and context.
     *
     * @param context the context used for routing RPCs to this implementation.
     * @param path the path identifier for which to register.
     */
    void registerPath(C context, P path);

    /**
     * Unregisters the RPC implementation associated with this registration for the given path
     * identifier and context.
     *
     * @param context the context used for routing RPCs to this implementation.
     * @param path the path identifier for which to unregister.
     */
    void unregisterPath(C context, P path);

    @Override
    void close();
}
