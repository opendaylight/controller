/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Executor of Three Phase Commit coordination for
 * {@link DOMDataWriteTransaction} transactions.
 *
 * Implementations are responsible for executing implementation of three-phase
 * commit protocol on supplied {@link DOMStoreThreePhaseCommitCohort}s.
 *
 *
 */
interface DOMDataCommitExecutor {

    /**
     * Submits supplied transaction to be executed in context of provided
     * cohorts.
     *
     * Transaction is used only as a context, cohorts should be associated with
     * this transaction.
     *
     * @param tx
     *            Transaction to be used as context for reporting
     * @param cohort
     *            DOM Store cohorts representing provided transaction, its
     *            subtransactoins.
     * @param listener
     *            Error listener which should be notified if transaction failed.
     * @return a CheckedFuture. if commit coordination on cohorts finished successfully,
     *         nothing is returned from the Future, On failure,
     *         the Future fails with a {@link TransactionCommitFailedException}.
     *
     */
    CheckedFuture<Void,TransactionCommitFailedException> submit(DOMDataWriteTransaction tx,
            Iterable<DOMStoreThreePhaseCommitCohort> cohort, Optional<DOMDataCommitErrorListener> listener);

}
