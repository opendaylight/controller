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
import com.google.common.util.concurrent.CheckedFuture;

/**
 *
 * Implementation prototype of commit method for
 * {@link DOMForwardedWriteTransaction}.
 *
 */
public interface DOMDataCommitImplementation {

    /**
     * User-supplied implementation of {@link DOMDataWriteTransaction#submit()}
     * for transaction.
     *
     * Callback invoked when {@link DOMDataWriteTransaction#submit()} is invoked
     * on transaction created by this factory.
     *
     * @param transaction
     *            Transaction on which {@link DOMDataWriteTransaction#commit()}
     *            was invoked.
     * @param cohorts
     *            Iteration of cohorts for subtransactions associated with
     *            commited transaction.
     *
     */
    CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts);
}

