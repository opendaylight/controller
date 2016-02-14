/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class CreateTransaction extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private int transactionType;
    private String transactionChainId;

    public CreateTransaction() {
    }

    public CreateTransaction(String transactionId, int transactionType, String transactionChainId,
            short version) {
        super(version);
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.transactionType = transactionType;
        this.transactionChainId = transactionChainId != null ? transactionChainId : "";
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getTransactionType() {
        return transactionType;
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = in.readUTF();
        transactionType = in.readInt();
        transactionChainId = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(transactionId);
        out.writeInt(transactionType);
        out.writeUTF(transactionChainId);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return ShardTransactionMessages.CreateTransaction.newBuilder().setTransactionId(transactionId)
                .setTransactionType(transactionType).setTransactionChainId(transactionChainId)
                .setMessageVersion(getVersion()).build();
    }

    @Override
    public String toString() {
        return "CreateTransaction [transactionId=" + transactionId + ", transactionType=" + transactionType
                + ", transactionChainId=" + transactionChainId + "]";
    }

    public static CreateTransaction fromSerializable(Object message) {
        if(message instanceof CreateTransaction) {
            return (CreateTransaction)message;
        } else {
            ShardTransactionMessages.CreateTransaction createTransaction =
                    (ShardTransactionMessages.CreateTransaction) message;
            return new CreateTransaction(createTransaction.getTransactionId(),
                    createTransaction.getTransactionType(), createTransaction.getTransactionChainId(),
                    (short)createTransaction.getMessageVersion());
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CreateTransaction || message instanceof ShardTransactionMessages.CreateTransaction;
    }
}
