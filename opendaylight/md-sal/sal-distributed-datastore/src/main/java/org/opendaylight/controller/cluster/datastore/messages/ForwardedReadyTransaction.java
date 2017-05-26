/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ReadWriteShardDataTreeTransaction;

/**
 * Transaction ReadyTransaction message that is forwarded to the local Shard from the ShardTransaction.
 *
 * @author Thomas Pantelis
 */
public class ForwardedReadyTransaction {
    private final TransactionIdentifier transactionId;
    private final ReadWriteShardDataTreeTransaction transaction;
    private final boolean doImmediateCommit;
    private final short txnClientVersion;

    public ForwardedReadyTransaction(TransactionIdentifier transactionId, short txnClientVersion,
            ReadWriteShardDataTreeTransaction transaction, boolean doImmediateCommit) {
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.txnClientVersion = txnClientVersion;
        this.doImmediateCommit = doImmediateCommit;
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    public ReadWriteShardDataTreeTransaction getTransaction() {
        return transaction;
    }

    public short getTxnClientVersion() {
        return txnClientVersion;
    }

    public boolean isDoImmediateCommit() {
        return doImmediateCommit;
    }

    @Override
    public String toString() {
        return "ForwardedReadyTransaction [transactionId=" + transactionId + ", doImmediateCommit=" + doImmediateCommit
                + ", txnClientVersion=" + txnClientVersion + "]";
    }
}
