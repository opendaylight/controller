package org.opendaylight.controller.sal.core.spi.data;

/**
 * A chain of transactions. Transactions in a chain need to be committed in
 * sequence and each transaction should see the effects of previous transactions
 * as if they happened. A chain makes no guarantees of atomicity, in fact
 * transactions are committed as soon as possible.
 *
 */
public interface DOMStoreTransactionChain extends DOMStoreTransactionFactory, AutoCloseable {

    /**
     * Create a new read only transaction which will continue the chain. The
     * previous read-write transaction has to be either READY or CANCELLED.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not COMMITED or CANCELLED, or
     *             if the chain has been closed.
     */
    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction();

    /**
     * Create a new read write transaction which will continue the chain. The
     * previous read-write transaction has to be either COMMITED or CANCELLED.
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not COMMITED or CANCELLED, or
     *             if the chain has been closed.
     */
    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction();

    /**
     * Create a new read write transaction which will continue the chain. The
     * previous read-write transaction has to be either COMMITED or CANCELLED.
     *
     *
     * @return New transaction in the chain.
     * @throws IllegalStateException
     *             if the previous transaction was not COMMITED or CANCELLED, or
     *             if the chain has been closed.
     */
    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction();

    @Override
    public void close();

}
