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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class CreateTransaction extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private TransactionIdentifier<?> transactionId;
    private int transactionType;

    public CreateTransaction() {
    }

    public CreateTransaction(TransactionIdentifier<?> transactionId, int transactionType, short version) {
        super(version);
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.transactionType = transactionType;
    }

    public TransactionIdentifier<?> getTransactionId() {
        return transactionId;
    }

    public int getTransactionType() {
        return transactionType;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = (TransactionIdentifier<?>) in.readObject();
        transactionType = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(transactionId);
        out.writeInt(transactionType);
    }

    @Override
    public String toString() {
        return "CreateTransaction [transactionId=" + transactionId + ", transactionType=" + transactionType + "]";
    }

    public static CreateTransaction fromSerializable(Object message) {
        Preconditions.checkArgument(message instanceof CreateTransaction);
        return (CreateTransaction)message;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CreateTransaction;
    }
}
