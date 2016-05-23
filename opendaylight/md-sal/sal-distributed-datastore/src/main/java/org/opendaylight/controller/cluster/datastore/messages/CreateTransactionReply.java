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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class CreateTransactionReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private String transactionPath;
    private TransactionIdentifier<?> transactionId;

    public CreateTransactionReply() {
    }

    public CreateTransactionReply(final String transactionPath, final TransactionIdentifier<?> transactionId,
            final ABIVersion version) {
        super(version);
        this.transactionPath = Preconditions.checkNotNull(transactionPath);
        this.transactionId = Preconditions.checkNotNull(transactionId);
    }

    public String getTransactionPath() {
        return transactionPath;
    }

    public TransactionIdentifier<?> getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = (TransactionIdentifier<?>) in.readObject();
        transactionPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(transactionId);
        out.writeUTF(transactionPath);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CreateTransactionReply [transactionPath=").append(transactionPath).append(", transactionId=")
                .append(transactionId).append(", version=").append(getVersion()).append("]");
        return builder.toString();
    }

    public static CreateTransactionReply fromSerializable(Object serializable) {
        Preconditions.checkNotNull(serializable instanceof CreateTransactionReply);
        return (CreateTransactionReply)serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CreateTransactionReply;
    }
}
