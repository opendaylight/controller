/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.Nullable;
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
    private final @Nullable SortedSet<String> participatingShardNames;

    public ForwardedReadyTransaction(final TransactionIdentifier transactionId, final short txnClientVersion,
            final ReadWriteShardDataTreeTransaction transaction, final boolean doImmediateCommit,
            final Optional<SortedSet<String>> participatingShardNames) {
        this.transactionId = requireNonNull(transactionId);
        this.transaction = requireNonNull(transaction);
        this.txnClientVersion = txnClientVersion;
        this.doImmediateCommit = doImmediateCommit;
        this.participatingShardNames = requireNonNull(participatingShardNames).orElse(null);
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

    public Optional<SortedSet<String>> getParticipatingShardNames() {
        return Optional.ofNullable(participatingShardNames);
    }

    @Override
    public String toString() {
        return "ForwardedReadyTransaction [transactionId=" + transactionId + ", transaction=" + transaction
                + ", doImmediateCommit=" + doImmediateCommit + ", participatingShardNames=" + participatingShardNames
                + ", txnClientVersion=" + txnClientVersion + "]";
    }
}
