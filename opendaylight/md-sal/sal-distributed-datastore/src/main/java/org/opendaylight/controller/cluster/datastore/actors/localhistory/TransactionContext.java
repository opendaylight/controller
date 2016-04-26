/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.messages.PersistTransactionRequest;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

final class TransactionContext {
    private final GlobalTransactionIdentifier gtid;
    private final DataTreeModification mod;
    private long expectedRequest = 0;
    private boolean sealed;

    TransactionContext(final GlobalTransactionIdentifier gtid, final DataTreeModification tx) {
        this.gtid = Preconditions.checkNotNull(gtid);
        this.mod = Preconditions.checkNotNull(tx);
    }

    private void checkNotSealed() {
        Preconditions.checkState(!sealed, "Context %s has been sealed", this);
    }

    RecordedTransaction seal() {
        checkNotSealed();
        sealed = true;
        return new RecordedTransaction(expectedRequest - 1);
    }

    GlobalTransactionIdentifier getGlobalIdentifier() {
        return gtid;
    }

    long getTransactionId() {
        return gtid.getTransactionId().getTransactionId();
    }

    long getExpectedRequest() {
        return expectedRequest;
    }

    void setExpectedRequest(final long expected) {
        expectedRequest = expected;
    }

    DataTreeModification nextTransaction() {
        return mod.newModification();
    }

    Optional<NormalizedNode<?, ?>> read(final YangInstanceIdentifier path) {
        checkNotSealed();
        return mod.readNode(path);
    }

    void applyOperation(final TransactionModification op) {
        if (op instanceof TransactionDelete) {
            mod.delete(op.getPath());
        } else if (op instanceof TransactionMerge) {
            mod.merge(op.getPath(), ((TransactionMerge) op).getData());
        } else if (op instanceof TransactionWrite) {
            mod.write(op.getPath(), ((TransactionWrite) op).getData());
        } else {
            throw new IllegalArgumentException("Unhandled operation " + op);
        }
    }

    PersistTransactionRequest toAbortRequest(final ActorRef replyTo, final Throwable cause) {
        return PersistTransactionRequest.createAbort(gtid, replyTo, cause);
    }

    PersistTransactionRequest toCanCommitRequest(final ActorRef replyTo) {
        return PersistTransactionRequest.createCanCommit(gtid, replyTo, mod);
    }

    PersistTransactionRequest toCommitRequest(final ActorRef replyTo) {
        return PersistTransactionRequest.createCommit(gtid, replyTo, mod);
    }
}
