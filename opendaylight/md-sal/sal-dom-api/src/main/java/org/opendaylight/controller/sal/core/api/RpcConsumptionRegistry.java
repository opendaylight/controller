/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.concurrent.Future;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

/**
 * @deprecated Use {@link org.opendaylight.controller.md.sal.dom.api.DOMRpcService} instead.
 */
@Deprecated
public interface RpcConsumptionRegistry {
    /**
     * Sends an RPC to other components registered to the broker.
     *
     * @see RpcImplementation
     * @param rpc
     *            Name of RPC
     * @param input
     *            Input data to the RPC
     * @return Result of the RPC call
     */
    Future<RpcResult<CompositeNode>> rpc(QName rpc, CompositeNode input);

}
