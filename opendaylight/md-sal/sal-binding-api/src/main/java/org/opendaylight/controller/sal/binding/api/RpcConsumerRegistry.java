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
 * RPC broker, which provides access to RPC invocation services.
 *
 * Base interface defining contract for retrieving MD-SAL version of RpcServices.
 *
 */
public interface RpcConsumerRegistry extends BindingAwareService {
    /**
     * Returns a instance (implementation) of requested YANG module
     * implementation / service provided by consumer.
     *
     * Returns implementation of requested RPC service, returned instance is
     * never an actual implementation of RPC service, but public proxy with
     * following behaviour of invocation of rpc methods:
     *
     * <ul>
     * <li>If implementation is registered to MD-SAL - all invocations are
     * forwarded to registered implementation of particular RPC.</li>
     * <li>If no implementation is available - invocation on any rpc method throws {@link IllegalStateException}.</li>
     * <li>If arguments are incorrect - throws {@link IllegalArgumentException}.
     * </ul>
     *
     * Returned public proxy is updated in background by RPC broker to
     * point to latest registered implementations.
     *
     * @return Public proxy for requested RPC service
     */
    <T extends RpcService> T getRpcService(Class<T> module);
}
