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

public class CreateTransactionReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private String transactionPath;
    private TransactionIdentifier transactionId;

    public CreateTransactionReply() {
    }

    public CreateTransactionReply(final String transactionPath, final TransactionIdentifier transactionId,
            final short version) {
        super(version);
        this.transactionPath = requireNonNull(transactionPath);
        this.transactionId = requireNonNull(transactionId);
    }

    public String getTransactionPath() {
        return transactionPath;
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = TransactionIdentifier.readFrom(in);
        transactionPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionId.writeTo(out);
        out.writeUTF(transactionPath);
    }

    @Override
    public String toString() {
        return "CreateTransactionReply [transactionPath=" + transactionPath
                + ", transactionId=" + transactionId
                + ", version=" + getVersion() + "]";
    }

    public static CreateTransactionReply fromSerializable(Object serializable) {
        checkArgument(serializable instanceof CreateTransactionReply);
        return (CreateTransactionReply)serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CreateTransactionReply;
    }
}
