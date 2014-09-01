/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

import com.google.common.util.concurrent.ListenableFuture;

public interface TransactionStatsTracker {

    void updateReadStatsAsync(ListenableFuture<?> readFuture);

    void addSuccessfulWrites(int n);

    void incrementFailedWrites();

    void addSuccessfulDeletes(int n);

    void incrementFailedDeletes();

    void incrementCanCommitPhaseFailures();

    void incrementPreCommitPhaseFailures();

    void incrementCommitPhaseFailures();

    void incrementOptimisticLockFailures();

    void addCommitDuration(long elapsedTime);
}