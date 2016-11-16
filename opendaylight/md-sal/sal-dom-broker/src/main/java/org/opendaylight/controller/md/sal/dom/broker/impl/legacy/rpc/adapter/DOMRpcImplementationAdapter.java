/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.rpc.adapter;

import com.google.common.base.Function;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

class DOMRpcImplementationAdapter<T extends org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation>
        implements DOMRpcImplementation {

    private final T implementation;

    private final HashMap<DOMRpcIdentifier, org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier> rpcMap;

    public DOMRpcImplementationAdapter(T implementation2,
            Set<org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier> rpcs) {
        this.implementation = implementation2;
        HashMap<DOMRpcIdentifier, org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier> map = new HashMap<>(rpcs.size());
        for (org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier rpc : rpcs) {
            DOMRpcIdentifier i = DOMRpcIdentifier.create(rpc.getType(), rpc.getContextReference());
            map.put(i, rpc);
        }
        rpcMap = (HashMap<DOMRpcIdentifier, org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier>) Collections
                .unmodifiableMap(map);
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(DOMRpcIdentifier rpc, NormalizedNode<?, ?> input) {

        CheckedFuture<org.opendaylight.controller.md.sal.dom.api.DOMRpcResult, org.opendaylight.controller.md.sal.dom.api.DOMRpcException> delegFuture = implementation
                .invokeRpc(rpcMap.get(rpc), input);

        ListenableFuture<DOMRpcResult> transformFuture = Futures.transform(delegFuture,
                (Function<? super org.opendaylight.controller.md.sal.dom.api.DOMRpcResult, DOMRpcResult>) result -> new DOMRpcResult() {

                    @Override
                    public NormalizedNode<?, ?> getResult() {
                        return result.getResult();
                    }

                    @Override
                    public Collection<RpcError> getErrors() {
                        return result.getErrors();
                    }
                });

        return Futures.makeChecked(transformFuture, result -> {
            org.opendaylight.controller.md.sal.dom.api.DOMRpcException exception =
                    (org.opendaylight.controller.md.sal.dom.api.DOMRpcException) result.getCause();
            return new DOMRpcImplementationNotAvailableException(exception.getMessage());
        });
    }

    Set<DOMRpcIdentifier> getRpcs() {
        return rpcMap.keySet();
    }
}
