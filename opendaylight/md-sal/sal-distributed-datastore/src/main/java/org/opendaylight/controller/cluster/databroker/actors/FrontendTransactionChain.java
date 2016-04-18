/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Queue;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

final class FrontendTransactionChain {
    private final Queue<FrontendTransaction> readyTransactions = new ArrayDeque<>();
    private final LocalHistoryIdentifier id;

    private FrontendTransaction currentTransaction;
    private long transactionCounter;

    FrontendTransactionChain(final LocalHistoryIdentifier id) {
        this.id = Preconditions.checkNotNull(id);
    }

    FrontendTransaction createTransaction() {
        Preconditions.checkState(currentTransaction == null, "Transaction {} is still open", currentTransaction);

        // currentTransaction = new FrontendTransaction(id, transactionCounter++);
        return currentTransaction;
    }





}
