/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of TransactionStatsTracker that does nothing.
 *
 * @author Thomas Pantelis
 */
public class NoopTransactionStatsTracker implements TransactionStatsTracker {

    @Override
    public void updateReadStatsAsync(ListenableFuture<Optional<NormalizedNode<?,?>>> readFuture) {
    }

    @Override
    public void addSuccessfulWrites(int n) {
    }

    @Override
    public void incrementFailedWrites() {
    }

    @Override
    public void addSuccessfulDeletes(int n) {
    }

    @Override
    public void incrementFailedDeletes() {
    }

    @Override
    public void incrementCanCommitPhaseFailures() {
    }

    @Override
    public void incrementPreCommitPhaseFailures() {
    }

    @Override
    public void incrementCommitPhaseFailures() {
    }

    @Override
    public void incrementOptimisticLockFailures() {
    }

    @Override
    public void addCommitDuration(long elapsedTime) {
    }
}
