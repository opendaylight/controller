package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCohort;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCohort.CohortTransaction;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.DataValidationFailedException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

class BindingDOMDataCommitCohortAdapter implements DOMDataCommitCohort {

    private final LogicalDatastoreType datastoreType;
    private final DataCommitCohort cohort;
    private final BindingToNormalizedNodeCodec codec;

    private BindingDOMDataCommitCohortAdapter(final LogicalDatastoreType datastoreType, final DataCommitCohort cohort,
            final BindingToNormalizedNodeCodec codec) {
        this.datastoreType = Preconditions.checkNotNull(datastoreType);
        this.cohort = Preconditions.checkNotNull(cohort);
        this.codec = Preconditions.checkNotNull(codec);
    }

    protected static DOMDataCommitCohort create(final LogicalDatastoreType datastoreType, final DataCommitCohort cohort,
            final BindingToNormalizedNodeCodec codec) {
        return new BindingDOMDataCommitCohortAdapter(datastoreType,cohort,codec);
    }

    @Override
    public CommitCohortTransaction canCommit(final Object txId, final DataTreeCandidate candidate, final SchemaContext ctx)
            throws DataValidationFailedException {
        final DataTreeModification bindingCandidate = LazyDataTreeModification.create(codec, candidate, datastoreType);
        final CohortTransaction bindingTx = cohort.canCommit(txId, bindingCandidate);
        if(bindingTx == DataCommitCohort.COHORT_NOOP_TRANSACTION) {
            return DOMDataCommitCohort.COHORT_NOOP_TRANSACTION;
        }
        return new BindingCommitCohortTransactionAdapter(bindingTx);
    }
}
