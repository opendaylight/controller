/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class ReadyTransactionReply implements SerializableMessage {
    public static final Class<ShardTransactionMessages.ReadyTransactionReply> SERIALIZABLE_CLASS =
            ShardTransactionMessages.ReadyTransactionReply.class;

    public static final int CURRENT_VERSION = 1;

    private final String cohortPath;
    private final int version;

    public ReadyTransactionReply(String cohortPath) {
        this(cohortPath, CURRENT_VERSION);
    }

    public ReadyTransactionReply(String cohortPath, int version) {
        this.cohortPath = cohortPath;
        this.version = version;
    }

    public String getCohortPath() {
        return cohortPath;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public ShardTransactionMessages.ReadyTransactionReply toSerializable() {
        return ShardTransactionMessages.ReadyTransactionReply.newBuilder()
                .setActorPath(cohortPath)
                .setMessageVersion(version)
                .build();
    }

    public static ReadyTransactionReply fromSerializable(Object serializable) {
        ShardTransactionMessages.ReadyTransactionReply o =
                (ShardTransactionMessages.ReadyTransactionReply) serializable;

        return new ReadyTransactionReply(o.getActorPath(), o.getMessageVersion());
    }
}
