/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Provides access to registered Remote Procedure Call (RPC) services, which are
 * defined in YANG models.
 *
 * RPC implementation / services could be registered using
 * {@link RpcProviderRegistry}.
 *
 */
public interface RpcConsumerRegistry extends BindingAwareService {
    /**
     * Returns implementation of requested RPC service.
     *
     * <p>
     * he returned instance is not an actual implementation of the RPC service
     * interface, but a proxy implementation of the interface that forwards to
     * an actual implementation, if any.
     * <p>
     *
     * The following describes the behavior of the proxy when invoking RPC methods:
     * <ul>
     * <li>If implementation is registered to MD-SAL - all invocations are
     * forwarded to registered implementation of particular RPC.</li>
     * <li>If no implementation is available - invocation on any rpc method
     * throws {@link IllegalStateException}.</li>
     * <li>If arguments are incorrect - throws {@link IllegalArgumentException}.
     * </ul>
     *
     * The returned proxy is automatically updated with the most recent
     * registered implementations.
     *
     * @param service Interface of RPC Service
     * @return Public proxy for requested RPC service. This method never returns null.
     */
    <T extends RpcService> T getRpcService(Class<T> service);
}
