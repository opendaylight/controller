/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class DataExistsReply implements SerializableMessage {
    public static final Class<ShardTransactionMessages.DataExistsReply> SERIALIZABLE_CLASS =
            ShardTransactionMessages.DataExistsReply.class;

    private static final DataExistsReply TRUE = new DataExistsReply(true, null);
    private static final DataExistsReply FALSE = new DataExistsReply(false, null);
    private static final ShardTransactionMessages.DataExistsReply SERIALIZABLE_TRUE =
            ShardTransactionMessages.DataExistsReply.newBuilder().setExists(true).build();
    private static final ShardTransactionMessages.DataExistsReply SERIALIZABLE_FALSE =
            ShardTransactionMessages.DataExistsReply.newBuilder().setExists(false).build();

    private final boolean exists;

    private DataExistsReply(final boolean exists, final Void dummy) {
        this.exists = exists;
    }

    /**
     * @deprecated Use {@link #create(boolean)} instead.
     * @param exists
     */
    @Deprecated
    public DataExistsReply(final boolean exists) {
        this(exists, null);
    }

    public static DataExistsReply create(final boolean exists) {
        return exists ? TRUE : FALSE;
    }

    public boolean exists() {
        return exists;
    }

    @Override
    public Object toSerializable() {
        return exists ? SERIALIZABLE_TRUE : SERIALIZABLE_FALSE;
    }

    public static DataExistsReply fromSerializable(final Object serializable) {
        ShardTransactionMessages.DataExistsReply o = (ShardTransactionMessages.DataExistsReply) serializable;
        return create(o.getExists());
    }
}
