/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

/**
 * {@link Provider}'s implementation of an RPC.
 *
 * In order to expose an RPC to other components, the provider MUST register
 * a concrete implementation of this interface.
 *
 * The registration could be done by :
 * <ul>
 * <li>returning an instance of implementation in the return value of
 * {@link Provider#getProviderFunctionality()}
 * <li>passing an instance of implementation and {@link QName} of rpc as
 * arguments to the
 * {@link org.opendaylight.controller.sal.core.api.Broker.ProviderSession#addRpcImplementation(QName, RpcImplementation)}
 * </ul>
 *
 * The simplified process of the invocation of rpc is following:
 *
 * <ol>
 * <li> {@link Consumer} invokes
 * {@link org.opendaylight.controller.sal.core.api.Broker.ConsumerSession#rpc(QName, CompositeNode)}
 * <li> {@link Broker} finds registered {@link RpcImplementation}s
 * <li> {@link Broker} invokes
 * {@link RpcImplementation#invokeRpc(QName, CompositeNode)}
 * <li> {@link RpcImplementation} processes the data and returns a
 * {@link RpcResult}
 * <li> {@link Broker} returns the {@link RpcResult} to {@link Consumer}
 * </ol>
 *
 * @deprecated Use {@link org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation} instead.
 */
@Deprecated
public interface RpcImplementation extends Provider.ProviderFunctionality {

    /**
     * A set of rpc types supported by implementation.
     *
     * The set of rpc {@link QName}s which are supported by this implementation.
     * This set is used, when {@link Provider} is registered to the SAL, to
     * register and expose the implementation of the returned rpcs.
     *
     * @return Set of QNames identifying supported RPCs
     */
    Set<QName> getSupportedRpcs();

    /**
     * Invokes a implementation of specified RPC asynchronously.
     *
     * @param rpc
     *            RPC to be invoked
     * @param input
     *            Input data for the RPC.
     *
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If rpc is null.
     *             <li>If input is not <code>null</code> and
     *             <code>false == rpc.equals(input.getNodeType)</code>
     *             </ul>
     * @return Future promising an RpcResult containing the output of
     *         the RPC if was executed successfully, the list of errors
     *         otherwise.
     */
    ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input);
}
