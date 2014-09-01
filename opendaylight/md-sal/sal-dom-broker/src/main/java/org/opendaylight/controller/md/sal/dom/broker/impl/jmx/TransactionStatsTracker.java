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
 * Interface for tracking and updating various transaction statistics provided via the
 * {@link TransactionStatsMXBean} interface.
 *
 * @author Thomas Pantelis
 * @see TransactionStatsMXBean
 */
public interface TransactionStatsTracker {

    void updateReadStatsAsync(ListenableFuture<Optional<NormalizedNode<?,?>>> readFuture);

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