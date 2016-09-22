/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * User-supplied participant in three-phase commit of transaction for configuration data tree
 *
 * Client-supplied implementation of commit handler for subtree, which
 * is responsible for processing CAN-COMMIT phase of three-phase commit protocol
 * and return CommitCohort, which provides access to additional transitions
 * such as PRE-COMMIT, COMMIT and ABORT.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncConfigurationCommitHandler<P extends Path<P>, D> {

    /**
     *
     * Requests a can commit phase
     *
     * Implementations SHOULD NOT do any blocking operation during
     * processing this callback.
     *
     * <b>Implementation Notes</b>
     * <ul>
     * <li>Implementation are REQUIRED to use <code>request</code> object for any data related access</li>
     * <li>Implementations SHOULD NOT use any other state stored outside configuration subtree for validation</li>
     * <li>Validation should happen asynchronously, outside callback call by updating returned {@link CheckedFuture}
     *     object.</li>
     * <li>If validation (CAN_COMMIT) phase:
     * <ul>
     * <li><b>is successful</b> - invocation of {@link CheckedFuture#checkedGet()} on returned future MUST
     *     return {@link AsyncConfigurationCommitCohort} associated with request.</li>
     * <li><b>is unsuccessful</b> - invocation of {@link CheckedFuture#checkedGet()} must throw instance of {@link DataValidationFailedException}
     * with human readable explanaition of error condition.
     * </li>
     * </ul>
     * </li>
     * @param request
     *            Commit Request submitted by client, which contains
     *            information about modifications and read-only view as
     *            if transaction happened.
     * @return CheckedFuture which contains client-supplied implementation of {@link AsyncConfigurationCommitCohort}
     *         associated with submitted request, if can commit phase is
     *         successful, if can commit was unsuccessful, future must fail with
     *         {@link TransactionCommitFailedException} exception.
     */
    CheckedFuture<AsyncConfigurationCommitCohort<P, D>, DataValidationFailedException> canCommit(
            ConfigurationCommitRequest<P, D> request);

    /**
     *
     * Commit Request as was submitted by client code
     *
     * Commit Request contains list view of created / updated / removed
     * path and read-only view of proposed client transaction,
     * which may be used to retrieve modified or referenced data.
     *
     *
     * @param <P>
     *            Type of path (subtree identifier), which represents location
     *            in tree
     * @param <D>
     *            Type of data (payload), which represents data payload
     */
    interface ConfigurationCommitRequest<P extends Path<P>, D> {

        /**
         *
         * Read-only transaction which provides access only to configuration
         * data tree as if submitted transaction successfully happened and
         * no other concurrent modifications happened between allocation
         * of client transactions and write of client transactions.
         *
         * Implementations of Commit Handlers are REQUIRED to use this
         * read-only view to access any data from configuration data tree,
         * in order to capture them as preconditions for this transaction.
         *
         * @return Read-only transaction which provides access only to configuration
         * data tree as if submitted transaction successfully happened
         */
        AsyncReadTransaction<P, D> getReadOnlyView();

        /**
         *
         * Returns iteration of paths, to data which was introduced by this transaction.
         *
         * @return Iteration of paths, which was introduced by this transaction.
         */
        Iterable<P> getCreatedPaths();
        /**
         *
         * Returns iteration of paths, to data which was updated by this transaction.
         *
         * @return Iteration of paths, which was updated by this transaction.
         */
        Iterable<P> getUpdatedPaths();

        /**
         *
         * Returns iteration of paths, to data which was removed by this transaction.
         *
         * @return Iteration of paths, which was removed by this transaction.
         */
        Iterable<P> getRemovedPaths();
    }

}
