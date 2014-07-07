package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A chain of transactions. Transactions in a chain need to be committed in
 * sequence and each transaction should see the effects of previous transactions
 * as if they happened. A chain makes no guarantees of atomicity, in fact
 * transactions are committed as soon as possible, but in order as they were
 * allocated.
 * <p>
 * For more information about transaction chaining and transaction chains
 * see {@link TransactionChain}.
 *
 * @see TransactionChain
 *
 */
public interface BindingTransactionChain extends TransactionChain<InstanceIdentifier<?>, DataObject> {

    /**
     * {@inheritDoc}
     */
    @Override
    ReadOnlyTransaction newReadOnlyTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    ReadWriteTransaction newReadWriteTransaction();

    /**
     * {@inheritDoc}
     */
    @Override
    WriteTransaction newWriteOnlyTransaction();

}
