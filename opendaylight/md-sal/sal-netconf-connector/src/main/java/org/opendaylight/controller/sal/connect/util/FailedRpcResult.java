/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.util;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public final class FailedRpcResult<T> implements RpcResult<T> {

    private final RpcError rpcError;

    public FailedRpcResult(final RpcError rpcError) {
        this.rpcError = rpcError;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    @Override
    public T getResult() {
        return null;
    }

    @Override
    public Collection<RpcError> getErrors() {
        return Collections.singletonList(rpcError);
    }
}
