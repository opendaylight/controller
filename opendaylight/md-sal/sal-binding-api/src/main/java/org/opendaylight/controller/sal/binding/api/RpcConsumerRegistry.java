/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Provides access to registered Remote Procedure Call (RPC) service implementations. The RPCs are
 * defined in YANG models.
 * <p>
 * RPC implementations are registered using the {@link RpcProviderRegistry}.
 *
 */
public interface RpcConsumerRegistry extends BindingAwareService, BindingService {
    /**
     * Returns an implementation of a requested RPC service.
     *
     * <p>
     * The returned instance is not an actual implementation of the RPC service
     * interface, but a proxy implementation of the interface that forwards to
     * an actual implementation, if any.
     * <p>
     *
     * The following describes the behavior of the proxy when invoking RPC methods:
     * <ul>
     * <li>If an actual implementation is registered with the MD-SAL, all invocations are
     * forwarded to the registered implementation.</li>
     * <li>If no actual implementation is registered, all invocations will fail by
     * throwing {@link IllegalStateException}.</li>
     * <li>Prior to invoking the actual implementation, the method arguments are are validated.
     * If any are invalid, an {@link IllegalArgumentException} is thrown.
     * </ul>
     *
     * The returned proxy is automatically updated with the most recent
     * registered implementation.
     * <p>
     * The generated RPC method APIs require implementors to return a {@link java.util.concurrent.Future Future}
     * instance that wraps the {@link org.opendaylight.yangtools.yang.common.RpcResult RpcResult}. Since
     * RPC methods may be implemented asynchronously, callers should avoid blocking on the
     * {@link java.util.concurrent.Future Future} result. Instead, it is recommended to use
     * {@link com.google.common.util.concurrent.JdkFutureAdapters#listenInPoolThread(java.util.concurrent.Future)}
     * or {@link com.google.common.util.concurrent.JdkFutureAdapters#listenInPoolThread(java.util.concurrent.Future, java.util.concurrent.Executor)}
     * to listen for Rpc Result. This will asynchronously listen for future result in executor and
     * will not block current thread.
     *
     * <pre>
     *   final Future<RpcResult<SomeRpcOutput>> future = someRpcService.someRpc( ... );
     *   Futures.addCallback(JdkFutureAdapters.listenInThreadPool(future), new FutureCallback<RpcResult<SomeRpcOutput>>() {
     *
     *       public void onSuccess(RpcResult<SomeRpcOutput> result) {
     *          // process result ...
     *       }
     *
     *       public void onFailure(Throwable t) {
     *          // RPC failed
     *       }
     *   );
     * </pre>
     * @param serviceInterface the interface of the RPC Service. Typically this is an interface generated
     *                         from a YANG model.
     * @return the proxy for the requested RPC service. This method never returns null.
     */
    <T extends RpcService> T getRpcService(Class<T> serviceInterface);
}
