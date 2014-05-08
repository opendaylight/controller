/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DummyFuture<T> implements Future<RpcResult<T>> {

    private final boolean cancel;
    private final boolean isCancelled;
    private final boolean isDone;
    private final RpcResult<T> result;

    public DummyFuture() {
        cancel = false;
        isCancelled = false;
        isDone = false;
        result = null;
    }

    private DummyFuture(final Builder<T> builder) {
        cancel = builder.cancel;
        isCancelled = builder.isCancelled;
        isDone = builder.isDone;
        result = builder.result;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return cancel;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public RpcResult<T> get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public RpcResult<T> get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        return result;
    }

    public static class Builder<T> {

        private boolean cancel;
        private boolean isCancelled;
        private boolean isDone;
        private RpcResult<T> result;

        public Builder<T> cancel(final boolean cancel) {
            this.cancel = cancel;
            return this;
        }

        public Builder<T> isCancelled(final boolean isCancelled) {
            this.isCancelled = isCancelled;
            return this;
        }

        public Builder<T> isDone(final boolean isDone) {
            this.isDone = isDone;
            return this;
        }

        public Builder<T> rpcResult(final RpcResult<T> result) {
            this.result = result;
            return this;
        }

        public Future<RpcResult<T>> build() {
            return new DummyFuture<T>(this);
        }
    }
}
