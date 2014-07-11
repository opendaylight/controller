/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.rpc.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public abstract class AbstractRpcExecutor implements RpcExecutor {
    private final RpcDefinition rpcDef;

    public AbstractRpcExecutor(RpcDefinition rpcDef) {
        this.rpcDef = rpcDef;
    }

    @Override
    public RpcDefinition getRpcDefinition() {
        return rpcDef;
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc(CompositeNode rpcRequest) throws RestconfDocumentedException {
        try {
            return getRpcResult(invokeRpcUnchecked(rpcRequest));
        } catch (IllegalArgumentException e) {
            throw new RestconfDocumentedException(e.getMessage(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        } catch (UnsupportedOperationException e) {
            throw new RestconfDocumentedException(e.getMessage(), ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED);
        } catch (Exception e) {
            throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.", e);
        }
    }

    protected abstract Future<RpcResult<CompositeNode>> invokeRpcUnchecked(CompositeNode rpcRequest);

    protected RpcResult<CompositeNode> getRpcResult(Future<RpcResult<CompositeNode>> fromFuture) {
        try {
            return fromFuture.get();
        } catch (InterruptedException e) {
            throw new RestconfDocumentedException(
                    "The operation was interrupted while executing and did not complete.", ErrorType.RPC,
                    ErrorTag.PARTIAL_OPERATION);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CancellationException) {
                throw new RestconfDocumentedException("The operation was cancelled while executing.", ErrorType.RPC,
                        ErrorTag.PARTIAL_OPERATION);
            } else if (cause != null) {
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                if (cause instanceof IllegalArgumentException) {
                    throw new RestconfDocumentedException(cause.getMessage(), ErrorType.PROTOCOL,
                            ErrorTag.INVALID_VALUE);
                }

                throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.",
                        cause);
            } else {
                throw new RestconfDocumentedException("The operation encountered an unexpected error while executing.",
                        e);
            }
        }
    }
}