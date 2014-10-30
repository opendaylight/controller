/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

/**
 * Transaction ReadyTransaction message that is forwarded to the local Shard from the ShardTransaction.
 *
 * @author Thomas Pantelis
 */
public class ForwardedReadyTransaction {
    private final String transactionID;
    private final DOMStoreThreePhaseCommitCohort cohort;
    private final Modification modification;
    private final boolean returnSerialized;
    private final int txnClientVersion;

    public ForwardedReadyTransaction(String transactionID, int txnClientVersion,
            DOMStoreThreePhaseCommitCohort cohort, Modification modification,
            boolean returnSerialized) {
        this.transactionID = transactionID;
        this.cohort = cohort;
        this.modification = modification;
        this.returnSerialized = returnSerialized;
        this.txnClientVersion = txnClientVersion;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public DOMStoreThreePhaseCommitCohort getCohort() {
        return cohort;
    }

    public Modification getModification() {
        return modification;
    }

    public boolean isReturnSerialized() {
        return returnSerialized;
    }

    public int getTxnClientVersion() {
        return txnClientVersion;
    }
}
