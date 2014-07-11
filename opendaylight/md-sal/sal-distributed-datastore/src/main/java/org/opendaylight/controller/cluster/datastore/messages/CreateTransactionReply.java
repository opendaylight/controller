/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class CreateTransactionReply implements SerializableMessage {

    public static final Class SERIALIZABLE_CLASS = ShardTransactionMessages.CreateTransactionReply.class;
    private final String transactionPath;
    private final String transactionId;

    public CreateTransactionReply(String transactionPath,
        String transactionId) {
        this.transactionPath = transactionPath;
        this.transactionId = transactionId;
    }

    public String getTransactionPath() {
        return transactionPath;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Object toSerializable(){
        return ShardTransactionMessages.CreateTransactionReply.newBuilder()
            .setTransactionActorPath(transactionPath)
            .setTransactionId(transactionId)
            .build();
    }

    public static CreateTransactionReply fromSerializable(Object serializable){
        ShardTransactionMessages.CreateTransactionReply o = (ShardTransactionMessages.CreateTransactionReply) serializable;
        return new CreateTransactionReply(o.getTransactionActorPath(), o.getTransactionId());
    }

}
