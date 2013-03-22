/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Set;

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;

/**
 * {@link Provider}'s implementation of rpc.
 * 
 * In order to expose the rpc to other components, the provider MUST register
 * concrete implementation of this interface
 * 
 * The registration could be done by :
 * <ul>
 * <li>returning an instance of implementation in the return value of
 * {@link Provider#getProviderFunctionality()}
 * <li>passing an instance of implementation and {@link QName} of rpc as
 * arguments to the
 * {@link ProviderSession#addRpcImplementation(QName, RpcImplementation)}
 * </ul>
 * 
 * The simplified process of the invocation of rpc is following:
 * 
 * <ol>
 * <li> {@link Consumer} invokes
 * {@link ConsumerSession#rpc(QName, CompositeNode)}
 * <li> {@link Broker} finds registered {@link RpcImplementation}s
 * <li> {@link Broker} invokes
 * {@link RpcImplementation#invokeRpc(QName, CompositeNode)}
 * <li> {@link RpcImplementation} processes the data and returns a
 * {@link RpcResult}
 * <li> {@link Broker} returns the {@link RpcResult} to {@link Consumer}
 * </ol>
 * 
 * 
 */
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
     * Invokes a implementation of specified rpc.
     * 
     * 
     * @param rpc
     *            Rpc to be invoked
     * @param input
     *            Input data for rpc.
     * 
     * @throws IllegalArgumentException
     *             <ul>
     *             <li>If rpc is null.
     *             <li>If input is not <code>null</code> and
     *             <code>false == rpc.equals(input.getNodeType)</code>
     *             </ul>
     * @return RpcResult containing the output of rpc if was executed
     *         successfully, the list of errors otherwise.
     */
    RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input);

}
