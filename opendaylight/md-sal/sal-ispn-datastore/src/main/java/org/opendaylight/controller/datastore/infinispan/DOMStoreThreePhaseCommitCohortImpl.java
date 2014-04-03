package org.opendaylight.controller.datastore.infinispan;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

public class DOMStoreThreePhaseCommitCohortImpl implements DOMStoreThreePhaseCommitCohort {
    private final InfinispanDOMStoreTransaction transaction;

    public DOMStoreThreePhaseCommitCohortImpl(InfinispanDOMStoreTransaction transaction){
        this.transaction = transaction;
    }

    public ListenableFuture<Boolean> canCommit() {
        return null;
    }

    public ListenableFuture<Void> preCommit() {
        return null;

    }

    public ListenableFuture<Void> abort() {
        return null;
    }

    public ListenableFuture<Void> commit() {
        transaction.resumeWrappedTransaction();

        transaction.commitWrappedTransaction();

        return Futures.immediateFuture(null);
    }

}
