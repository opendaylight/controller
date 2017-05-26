/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;

/**
 * Message used to batch write, merge, delete modification operations to the  ShardTransaction actor.
 *
 * @author Thomas Pantelis
 */
public class BatchedModifications extends MutableCompositeModification {
    private static final long serialVersionUID = 1L;

    private boolean ready;
    private boolean doCommitOnReady;
    private int totalMessagesSent;
    private TransactionIdentifier transactionId;

    public BatchedModifications() {
    }

    public BatchedModifications(TransactionIdentifier transactionId, short version) {
        super(version);
        this.transactionId = Preconditions.checkNotNull(transactionId, "transactionID can't be null");
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isDoCommitOnReady() {
        return doCommitOnReady;
    }

    public void setDoCommitOnReady(boolean doCommitOnReady) {
        this.doCommitOnReady = doCommitOnReady;
    }

    public int getTotalMessagesSent() {
        return totalMessagesSent;
    }

    public void setTotalMessagesSent(int totalMessagesSent) {
        this.totalMessagesSent = totalMessagesSent;
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }


    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = TransactionIdentifier.readFrom(in);
        ready = in.readBoolean();
        totalMessagesSent = in.readInt();
        doCommitOnReady = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionId.writeTo(out);
        out.writeBoolean(ready);
        out.writeInt(totalMessagesSent);
        out.writeBoolean(doCommitOnReady);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BatchedModifications [transactionId=").append(transactionId).append(", ready=").append(ready)
            .append(", totalMessagesSent=").append(totalMessagesSent).append(", modifications size=")
            .append(getModifications().size()).append("]");
        return builder.toString();
    }
}
