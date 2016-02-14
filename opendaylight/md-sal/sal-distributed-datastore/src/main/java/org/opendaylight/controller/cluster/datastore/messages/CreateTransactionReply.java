/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class CreateTransactionReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private String transactionPath;
    private String transactionId;

    public CreateTransactionReply() {
    }

    public CreateTransactionReply(final String transactionPath, final String transactionId, final short version) {
        super(version);
        this.transactionPath = transactionPath;
        this.transactionId = transactionId;
    }

    public String getTransactionPath() {
        return transactionPath;
    }

    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = in.readUTF();
        transactionPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(transactionId);
        out.writeUTF(transactionPath);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return ShardTransactionMessages.CreateTransactionReply.newBuilder().setTransactionActorPath(transactionPath)
                .setTransactionId(transactionId).setMessageVersion(getVersion()).build();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CreateTransactionReply [transactionPath=").append(transactionPath).append(", transactionId=")
                .append(transactionId).append(", version=").append(getVersion()).append("]");
        return builder.toString();
    }

    public static CreateTransactionReply fromSerializable(Object serializable) {
        if(serializable instanceof CreateTransactionReply) {
            return (CreateTransactionReply)serializable;
        } else {
            ShardTransactionMessages.CreateTransactionReply o =
                    (ShardTransactionMessages.CreateTransactionReply) serializable;
            return new CreateTransactionReply(o.getTransactionActorPath(), o.getTransactionId(),
                    (short)o.getMessageVersion());
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CreateTransactionReply ||
                message instanceof ShardTransactionMessages.CreateTransactionReply;
    }
}
