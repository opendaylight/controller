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
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;

/**
 * Message used to batch write, merge, delete modification operations to the  ShardTransaction actor.
 *
 * @author Thomas Pantelis
 */
public class BatchedModifications extends MutableCompositeModification implements SerializableMessage {
    private static final long serialVersionUID = 1L;

    private boolean ready;
    private String transactionID;
    private String transactionChainID;

    public BatchedModifications() {
    }

    public BatchedModifications(String transactionID, short version, String transactionChainID) {
        super(version);
        this.transactionID = Preconditions.checkNotNull(transactionID, "transactionID can't be null");
        this.transactionChainID = transactionChainID != null ? transactionChainID : "";
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public String getTransactionChainID() {
        return transactionChainID;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionID = in.readUTF();
        transactionChainID = in.readUTF();
        ready = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(transactionID);
        out.writeUTF(transactionChainID);
        out.writeBoolean(ready);
    }

    @Override
    public Object toSerializable() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BatchedModifications [transactionID=").append(transactionID).append(", ready=").append(ready)
                .append(", modifications size=").append(getModifications().size()).append("]");
        return builder.toString();
    }
}
