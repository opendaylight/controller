/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

@Deprecated(since = "9.0.0", forRemoval = true)
public final class CreateTransaction extends VersionedExternalizableMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TransactionIdentifier transactionId;
    private int transactionType;

    public CreateTransaction() {
    }

    public CreateTransaction(final TransactionIdentifier transactionId, final int transactionType,
            final short version) {
        super(version);
        this.transactionId = requireNonNull(transactionId);
        this.transactionType = transactionType;
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    public int getTransactionType() {
        return transactionType;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = TransactionIdentifier.readFrom(in);
        transactionType = in.readInt();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionId.writeTo(out);
        out.writeInt(transactionType);
    }

    @Override
    public String toString() {
        return "CreateTransaction [transactionId=" + transactionId + ", transactionType=" + transactionType + "]";
    }

    public static CreateTransaction fromSerializable(final Object message) {
        checkArgument(message instanceof CreateTransaction);
        return (CreateTransaction)message;
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof CreateTransaction;
    }
}
