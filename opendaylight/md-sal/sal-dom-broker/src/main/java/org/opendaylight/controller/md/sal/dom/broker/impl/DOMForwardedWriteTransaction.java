package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

class DOMForwardedWriteTransaction<T extends DOMStoreWriteTransaction> extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {

    private final DOMDataCommitImplementation commitImpl;
    private ImmutableList<DOMStoreThreePhaseCommitCohort> cohorts;

    protected DOMForwardedWriteTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, T> backingTxs,
            final DOMDataCommitImplementation commitImpl) {
        super(identifier, backingTxs);
        this.commitImpl = commitImpl;
    }

    /**
     * Seals all backing {@link DOMStoreWriteTransaction} transactions to
     * ready.
     *
     */
    private synchronized Iterable<DOMStoreThreePhaseCommitCohort> ready() {
        checkState(cohorts == null, "Transaction was already marked as ready.");
        ImmutableList.Builder<DOMStoreThreePhaseCommitCohort> cohortsBuilder = ImmutableList.builder();
        for (DOMStoreWriteTransaction subTx : getSubtransactions()) {
            cohortsBuilder.add(subTx.ready());
        }
        cohorts = cohortsBuilder.build();
        return cohorts;
    }

    protected ImmutableList<DOMStoreThreePhaseCommitCohort> getCohorts() {
        return cohorts;
    }

    @Override
    public void put(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
        getSubtransaction(store).write(path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier path) {
        getSubtransaction(store).delete(path);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final InstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        getSubtransaction(store).merge(path, data);
    }

    @Override
    public void cancel() {
        // FIXME: Implement cancelation of tasks
    }

    @Override
    public synchronized ListenableFuture<RpcResult<TransactionStatus>> commit() {
        ready();
        return commitImpl.commit(this, getCohorts());
    }
}