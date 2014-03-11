package org.opendaylight.controller.md.sal.common.api.data;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface AsyncReadWriteTransaction<P extends Path<P>, D> extends AsyncTransaction<P, D>,
        WriteableTransaction<P, D>, DataChange<P, D> {


    @Override
    public TransactionStatus getStatus();

    /**
     * Cancels transaction.
     *
     * Transaction could be only cancelled if it's status
     * is {@link TransactionStatus#NEW} or {@link TransactionStatus#SUBMITED}
     *
     * Invoking cancel() on {@link TransactionStatus#FAILED} or {@link TransactionStatus#CANCELED}
     * will have no effect.
     *
     * @throws IllegalStateException If transaction status is {@link TransactionStatus#COMMITED}
     *
     */
    public void cancel();


    /**
     *
     * Closes transaction and resources allocated to the transaction.
     *
     * This call does not change Transaction status. Client SHOULD
     * explicitly {@link #commit()} or {@link #cancel()} transaction.
     *
     */
    @Override
    public void close();

    @Override
    public Future<RpcResult<TransactionStatus>> commit();

}
