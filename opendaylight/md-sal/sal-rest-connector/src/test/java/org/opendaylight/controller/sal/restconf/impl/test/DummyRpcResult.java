/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.Collection;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DummyRpcResult<T> implements RpcResult<T> {

    private final boolean isSuccessful;
    private final T result;
    private final Collection<RpcError> errors;

    public DummyRpcResult() {
        isSuccessful = false;
        result = null;
        errors = null;
    }

    private DummyRpcResult(final Builder<T> builder) {
        isSuccessful = builder.isSuccessful;
        result = builder.result;
        errors = builder.errors;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public Collection<RpcError> getErrors() {
        return errors;
    }

    public static class Builder<T> {
        private boolean isSuccessful;
        private T result;
        private Collection<RpcError> errors;

        public Builder<T> isSuccessful(final boolean isSuccessful) {
            this.isSuccessful = isSuccessful;
            return this;
        }

        public Builder<T> result(final T result) {
            this.result = result;
            return this;
        }

        public Builder<T> errors(final Collection<RpcError> errors) {
            this.errors = errors;
            return this;
        }

        public RpcResult<T> build() {
            return new DummyRpcResult<T>(this);
        }

    }

}
