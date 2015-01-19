/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class CloseTransactionReply implements SerializableMessage {
    public static final Class<ShardTransactionMessages.CloseTransactionReply> SERIALIZABLE_CLASS =
            ShardTransactionMessages.CloseTransactionReply.class;

    private static final Object SERIALIZED_INSTANCE =
            ShardTransactionMessages.CloseTransactionReply.newBuilder().build();

    public static final CloseTransactionReply INSTANCE = new CloseTransactionReply();

    @Override
    public Object toSerializable() {
        return SERIALIZED_INSTANCE;
    }
}
