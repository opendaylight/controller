/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;

abstract class Behavior {
    private GlobalTransactionIdentifier lastCommittedTx;

    abstract Behavior handleTransactionRequest(TransactionRequest request);

    void recordAbortedTransaction(final GlobalTransactionIdentifier transactionId) {
        // TODO Auto-generated method stub

    }
}