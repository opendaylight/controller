/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import scala.concurrent.Future;

/**
 * A {@link org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort}
 * instance given out for empty transactions.
 */
final class NoOpDOMStoreThreePhaseCommitCohort extends AbstractThreePhaseCommitCohort<Object> {
    static final NoOpDOMStoreThreePhaseCommitCohort INSTANCE = new NoOpDOMStoreThreePhaseCommitCohort();

    private NoOpDOMStoreThreePhaseCommitCohort() {
        // Hidden to prevent instantiation
    }

    @Override
    public ListenableFuture<Boolean> canCommit() {
        return IMMEDIATE_BOOLEAN_SUCCESS;
    }

    @Override
    public ListenableFuture<Void> preCommit() {
        return IMMEDIATE_VOID_SUCCESS;
    }

    @Override
    public ListenableFuture<Void> abort() {
        return IMMEDIATE_VOID_SUCCESS;
    }

    @Override
    public ListenableFuture<Void> commit() {
        return IMMEDIATE_VOID_SUCCESS;
    }

    @Override
    List<Future<Object>> getCohortFutures() {
        return Collections.emptyList();
    }
}
