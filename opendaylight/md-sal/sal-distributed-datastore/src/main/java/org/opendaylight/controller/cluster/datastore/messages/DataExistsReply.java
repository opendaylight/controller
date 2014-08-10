/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class DataExistsReply implements SerializableMessage{


    public static final Class SERIALIZABLE_CLASS = ShardTransactionMessages.DataExistsReply.class;

    private final boolean exists;

    public DataExistsReply(boolean exists) {
        this.exists = exists;
    }

    public boolean exists() {
        return exists;
    }

    @Override public Object toSerializable() {
        return ShardTransactionMessages.DataExistsReply.newBuilder()
            .setExists(exists).build();
    }

    public static DataExistsReply fromSerializable(Object serializable){
        ShardTransactionMessages.DataExistsReply o = (ShardTransactionMessages.DataExistsReply) serializable;
        return new DataExistsReply(o.getExists());
    }

}
