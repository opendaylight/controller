/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class ReadyTransaction implements SerializableMessage{
    public static final Class<ShardTransactionMessages.ReadyTransaction> SERIALIZABLE_CLASS =
            ShardTransactionMessages.ReadyTransaction.class;

    private static final Object SERIALIZED_INSTANCE = ShardTransactionMessages.ReadyTransaction.newBuilder().build();

    public static final ReadyTransaction INSTANCE = new ReadyTransaction();

    @Override
    public Object toSerializable() {
        return SERIALIZED_INSTANCE;
    }
}
