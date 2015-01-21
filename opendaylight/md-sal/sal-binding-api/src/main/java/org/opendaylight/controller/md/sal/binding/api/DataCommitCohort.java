/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.DataValidationFailedException;


/**
 *
 * Commit cohort participating in commit of data modification, which can
 * validate data tree modifications, with option to reject supplied
 * modification, and with callbacks describing state of commit.
 *
 * <h2>Performance implications</h2>
 *
 * {@link DataCommitCohort}s are hooked up into commit of data tree changes
 * and MAY negatively affect performance of data broker / store.
 *
 * Implementations of this interface are discouraged, unless you really need
 * ability to veto data tree changes, or to provide external state change in
 * sync with visibility of commited data.
 *
 *
 * <h2>Implementation requirements</h2>
 *
 * <h3>Correctness assumptions</h3>
 * Implementation SHOULD use only {@link DataRootModification} for validation
 * purposes.
 *
 * Use of any other external mutable state is discouraged, implementation MUST
 * NOT use any transaction related APIs on same data broker / data store
 * instance during invocation of callbacks, except ones provided as argument.
 *
 * Note that this MAY BE enforced by some implementations of
 * {@link DataBroker} or {@link DataCommitCoordinator} and such calls may fail.
 *
 *
 * <h3>DataTreeCandidate assumptions</h3> Implementation SHOULD NOT make any
 * assumptions on {@link DataTreeCandidate} being successfully committed until
 * associated {@link CohortTransaction} received
 * {@link CohortTransaction#commit()} callback was received.
 *
 *
 * <h2>Usage patterns</h2>
 *
 * <h3>Data Tree Validator</h3>
 *
 * Validator is implementation, which only validates {@link DataTreeCandidate}
 * and does not retain any state derived from edited data - does not care if
 * {@link DataTreeCandidate} was rejected afterwards or transaction was
 * cancelled.
 *
 * Implementation may opt-out from receiving {@code preCommit()},
 * {@code commit()}, {@code abort()} callbacks by returning
 * {@link #COHORT_NOOP_TRANSACTION}.
 *
 *
 * TODO: Provide example and describe more usage patterns
 *
 * @author Tony Tkacik &lt;ttkacik@cisco.com&gt;
 *
 */
@Beta
public interface DataCommitCohort {

    public static final CohortTransaction COHORT_NOOP_TRANSACTION = new CohortTransaction() {

        @Override
        public ListenableFuture<?> preCommit() {
            return Futures.immediateFuture(null);
        }

        @Override
        public ListenableFuture<?> commit() {
            return Futures.immediateFuture(null);
        }

        @Override
        public ListenableFuture<?> abort() {
            return Futures.immediateFuture(null);
        }
    };

    public interface CohortTransaction {

        /**
         * Initiates a pre-commit of associated request
         *
         * Implementation MUST NOT do any blocking calls during this callback, all
         * pre-commit preparation SHOULD happen asynchronously and MUST result in
         * completing returned future object.
         *
         * @return Future which is completed once pre-commit phase for this request
         *         is finished..
         *
         **/
        ListenableFuture<?> preCommit();

        /**
         *
         * Commits cohort transaction.
         *
         * This callback is invoked by commit coordinator
         * if associated data transaction finished {@link #preCommit()} phase
         * and will be commited.
         *
         * Implementation should make state, which were derived by implementation
         * from associated {@link DataTreeCandidate} visible-
         *
         * @return Listenable Future which will complete once commit is finished.
         */
        ListenableFuture<?> commit();

        /**
         * Aborts cohort transaction.
         *
         * This callback is invoked by commit coordinator
         * if associated data transaction will not be commited and should be aborted.
         *
         * Implementation MUST rollback any changes, which were
         * introduced by implementation based on supplied {@link DataTreeCandidate}.
         *
         * @return ListenableFuture which will complete once abort is completed.
         */
        ListenableFuture<?> abort();

    }

    /**
     * Validates supplied data tree candidate and associates cohort-specific transaction
     * with data broker transaction.
     *
     * If {@link DataValidationFailedException} is thrown by implementation,
     * commit of supplied data will be prevented, with the DataBroker transaction
     * providing the thrown exception as the cause of failure.
     *
     * Note this call is synchronous and it is executed during transaction commit,
     * so it affects performance characteristics of data broker.
     *
     * Implementation SHOULD do it processing fast, SHOULD NOT block on any external
     * resources.
     *
     * Implementation SHOULD NOT access any data transaction related APIs during
     * invocation of callback. Note that this may be enforced by some implementations
     * of {@link DataCommitCoordinator} and such calls may fail.
     *
     * Implementation MAY opt-out from receiving {@code preCommit()}, {@code commit()},
     * {@code abort()} callbacks by returning {@link #COHORT_NOOP_TRANSACTION}. Otherwise
     * implementation MUST return instance of {@link CohortTransaction},
     * which will be used for sending additional messages to coordinate commit of
     * data.
     *
     * @param txId Transaction identifier. SHOULD be used only for reporting and
     *          correlation. Implementation MUST NOT use {@code txId} for validation.
     * @param candidate
     *            Data Tree candidate to be validated and committed.
     * @return Commit Cohort Transaction which is associated with commit of supplied candidate data.
     *
     * @throws DataValidationFailedException
     *             If and only if provided {@link DataRootModification} did not
     *             pass validation. Users are encouraged to use more specific
     *             subclasses of this exception to provide additional information about
     *             validation failure reason.
     */
    @Nonnull
    CohortTransaction canCommit(@Nonnull Object txId, @Nonnull DataRootModification candidate)
            throws DataValidationFailedException;

}
