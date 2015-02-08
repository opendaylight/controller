/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class CreateTransactionReply implements SerializableMessage {

    public static final Class<?> SERIALIZABLE_CLASS = ShardTransactionMessages.CreateTransactionReply.class;
    private final String transactionPath;
    private final String transactionId;
    private final short version;

    public CreateTransactionReply(String transactionPath, String transactionId) {
        this(transactionPath, transactionId, DataStoreVersions.CURRENT_VERSION);
    }

    public CreateTransactionReply(final String transactionPath,
                                  final String transactionId, final short version) {
        this.transactionPath = transactionPath;
        this.transactionId = transactionId;
        this.version = version;
    }


    public String getTransactionPath() {
        return transactionPath;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public short getVersion() {
        return version;
    }

    @Override
    public Object toSerializable(){
        return ShardTransactionMessages.CreateTransactionReply.newBuilder()
            .setTransactionActorPath(transactionPath)
            .setTransactionId(transactionId)
            .setMessageVersion(version)
            .build();
    }

    public static CreateTransactionReply fromSerializable(Object serializable){
        ShardTransactionMessages.CreateTransactionReply o = (ShardTransactionMessages.CreateTransactionReply) serializable;
        return new CreateTransactionReply(o.getTransactionActorPath(), o.getTransactionId(),
                (short)o.getMessageVersion());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CreateTransactionReply [transactionPath=").append(transactionPath).append(", transactionId=")
                .append(transactionId).append(", version=").append(version).append("]");
        return builder.toString();
    }
}
