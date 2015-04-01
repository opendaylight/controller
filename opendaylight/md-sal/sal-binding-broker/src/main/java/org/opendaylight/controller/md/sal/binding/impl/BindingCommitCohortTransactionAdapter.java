package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCohort.CohortTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitCohort.CommitCohortTransaction;

class BindingCommitCohortTransactionAdapter implements CommitCohortTransaction {

    private final CohortTransaction delegate;

    public BindingCommitCohortTransactionAdapter(final CohortTransaction bindingTx) {
       this.delegate = bindingTx;
    }

    @Override
    public ListenableFuture<?> preCommit() {
        return delegate.preCommit();
    }

    @Override
    public ListenableFuture<?> commit() {
        return delegate.commit();
    }

    @Override
    public ListenableFuture<?> abort() {
        return delegate.abort();
    }

}
