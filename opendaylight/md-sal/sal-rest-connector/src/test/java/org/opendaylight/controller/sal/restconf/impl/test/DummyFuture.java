package org.opendaylight.controller.sal.restconf.impl.test;

import java.util.concurrent.*;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DummyFuture implements Future<RpcResult<TransactionStatus>> {

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public RpcResult<TransactionStatus> get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public RpcResult<TransactionStatus> get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return null;
    }
}