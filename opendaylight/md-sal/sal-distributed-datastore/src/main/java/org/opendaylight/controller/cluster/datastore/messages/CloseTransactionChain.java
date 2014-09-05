/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionChainMessages;

public class CloseTransactionChain implements SerializableMessage {
    public static final Class SERIALIZABLE_CLASS =
        ShardTransactionChainMessages.CloseTransactionChain.class;
    private final String transactionChainId;

    public CloseTransactionChain(String transactionChainId){
        this.transactionChainId = transactionChainId;
    }

    @Override
    public Object toSerializable() {
        return ShardTransactionChainMessages.CloseTransactionChain.newBuilder()
            .setTransactionChainId(transactionChainId).build();
    }

    public static CloseTransactionChain fromSerializable(Object message){
        ShardTransactionChainMessages.CloseTransactionChain closeTransactionChain
            = (ShardTransactionChainMessages.CloseTransactionChain) message;

        return new CloseTransactionChain(closeTransactionChain.getTransactionChainId());
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }
}
