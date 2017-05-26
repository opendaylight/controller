/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;

import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * Three phase Commit Cohort for subtree, which is
 * uniquely associated with user submitted transcation.
 *
 * @param <P>
 *            Type of path (subtree identifier), which represents location in
 *            tree
 * @param <D>
 *            Type of data (payload), which represents data payload
 */
public interface AsyncConfigurationCommitCohort<P extends Path<P>, D> {

    /**
     * Initiates a pre-commit of associated request
     *
     * Implementation MUST NOT do any blocking calls during this callback, all
     * pre-commit preparation SHOULD happen asynchronously and MUST result in
     * completing returned future object.
     *
     * @param rebasedTransaction
     *            Read-only view of transaction as if happened on top of actual
     *            data store
     * @return Future which is completed once pre-commit phase for this request
     *         is finished.
     */
    ListenableFuture<Void> preCommit(AsyncReadTransaction<P, D> rebasedTransaction);

    /**
     *
     * Initiates a commit phase of associated request
     *
     * Implementation MUST NOT do any blocking calls during this callback, all
     * commit finalization SHOULD happen asynchronously and MUST result in
     * completing returned future object.
     *
     * @return Future which is completed once commit phase for associated
     *         request is finished.
     */
    ListenableFuture<Void> commit();

    /**
     *
     * Initiates abort phase of associated request
     *
     * Implementation MUST NOT do any blocking calls during this callback, all
     * commit finalization SHOULD happen asynchronously and MUST result in
     * completing returned future object.
     *
     * @return Future which is completed once commit phase for associated
     *         request is finished.
     */
    ListenableFuture<Void> abort();

}
