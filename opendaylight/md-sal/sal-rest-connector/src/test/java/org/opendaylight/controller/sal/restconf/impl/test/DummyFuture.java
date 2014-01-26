/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.concurrent.*;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DummyFuture implements Future<RpcResult<TransactionStatus>> {
    
    private final boolean cancel;
    private final boolean isCancelled;
    private final boolean isDone;
    private final RpcResult<TransactionStatus> result;
    
    public DummyFuture() {
        cancel = false;
        isCancelled = false;
        isDone = false;
        result = null;
    }
    
    private DummyFuture(Builder builder) {
        cancel = builder.cancel;
        isCancelled = builder.isCancelled;
        isDone = builder.isDone;
        result = builder.result;
    }
    
    public static Builder builder() {
        return new DummyFuture.Builder();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
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
    public RpcResult<TransactionStatus> get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public RpcResult<TransactionStatus> get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return result;
    }
    
    public static class Builder {
        
        private boolean cancel;
        private boolean isCancelled;
        private boolean isDone;
        private RpcResult<TransactionStatus> result;

        public Builder cancel(boolean cancel) {
            this.cancel = cancel;
            return this;
        }
        
        public Builder isCancelled(boolean isCancelled) {
            this.isCancelled = isCancelled;
            return this;
        }
        
        public Builder isDone(boolean isDone) {
            this.isDone = isDone;
            return this;
        }
        
        public Builder rpcResult(RpcResult<TransactionStatus> result) {
            this.result = result;
            return this;
        }
        
        public Future<RpcResult<TransactionStatus>> build() {
            return new DummyFuture(this);
        }
    }
}
