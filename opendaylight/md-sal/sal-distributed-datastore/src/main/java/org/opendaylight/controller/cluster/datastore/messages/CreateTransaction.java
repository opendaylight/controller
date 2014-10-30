/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;


import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;


public class CreateTransaction implements SerializableMessage {
    public static final Class<ShardTransactionMessages.CreateTransaction> SERIALIZABLE_CLASS =
            ShardTransactionMessages.CreateTransaction.class;

    public static final int CURRENT_MESSAGE_VERSION = 1;

    private final String transactionId;
    private final int transactionType;
    private final String transactionChainId;
    private final int clientVersion;

    public CreateTransaction(String transactionId, int transactionType) {
        this(transactionId, transactionType, "");
    }

    public CreateTransaction(String transactionId, int transactionType, String transactionChainId) {
        this(transactionId, transactionType, transactionChainId, CURRENT_MESSAGE_VERSION);
    }

    private CreateTransaction(String transactionId, int transactionType, String transactionChainId,
            int clientVersion) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.transactionChainId = transactionChainId;
        this.clientVersion = clientVersion;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getTransactionType() {
        return transactionType;
    }

    public int getClientVersion() {
        return clientVersion;
    }

    @Override
    public Object toSerializable() {
        return ShardTransactionMessages.CreateTransaction.newBuilder()
            .setTransactionId(transactionId)
            .setTransactionType(transactionType)
            .setTransactionChainId(transactionChainId)
            .setMessageVersion(clientVersion).build();
    }

    public static CreateTransaction fromSerializable(Object message) {
        ShardTransactionMessages.CreateTransaction createTransaction =
            (ShardTransactionMessages.CreateTransaction) message;
        return new CreateTransaction(createTransaction.getTransactionId(),
            createTransaction.getTransactionType(), createTransaction.getTransactionChainId(),
            createTransaction.getMessageVersion());
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }
}
