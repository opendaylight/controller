/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.ShardDataTreeCohort;
import org.opendaylight.controller.cluster.datastore.modification.Modification;

/**
 * Transaction ReadyTransaction message that is forwarded to the local Shard from the ShardTransaction.
 *
 * @author Thomas Pantelis
 */
public class ForwardedReadyTransaction {
    private final String transactionID;
    private final ShardDataTreeCohort cohort;
    private final Modification modification;
    private final boolean returnSerialized;
    private final boolean doImmediateCommit;
    private final short txnClientVersion;

    public ForwardedReadyTransaction(String transactionID, short txnClientVersion,
            ShardDataTreeCohort cohort, Modification modification,
            boolean returnSerialized, boolean doImmediateCommit) {
        this.transactionID = transactionID;
        this.cohort = cohort;
        this.modification = modification;
        this.returnSerialized = returnSerialized;
        this.txnClientVersion = txnClientVersion;
        this.doImmediateCommit = doImmediateCommit;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public ShardDataTreeCohort getCohort() {
        return cohort;
    }

    public Modification getModification() {
        return modification;
    }

    public boolean isReturnSerialized() {
        return returnSerialized;
    }

    public short getTxnClientVersion() {
        return txnClientVersion;
    }

    public boolean isDoImmediateCommit() {
        return doImmediateCommit;
    }
}
