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
    private TransactionIdentifier<?> transactionID;

    public BatchedModifications() {
    }

    public BatchedModifications(TransactionIdentifier<?> transactionID, short version) {
        super(version);
        this.transactionID = Preconditions.checkNotNull(transactionID, "transactionID can't be null");
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

    public TransactionIdentifier<?> getTransactionID() {
        return transactionID;
    }


    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionID = (TransactionIdentifier<?>) in.readObject();
        ready = in.readBoolean();
        totalMessagesSent = in.readInt();
        doCommitOnReady = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(transactionID);
        out.writeBoolean(ready);
        out.writeInt(totalMessagesSent);
        out.writeBoolean(doCommitOnReady);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BatchedModifications [transactionID=").append(transactionID).append(", ready=").append(ready)
            .append(", totalMessagesSent=").append(totalMessagesSent).append(", modifications size=")
            .append(getModifications().size()).append("]");
        return builder.toString();
    }
}
