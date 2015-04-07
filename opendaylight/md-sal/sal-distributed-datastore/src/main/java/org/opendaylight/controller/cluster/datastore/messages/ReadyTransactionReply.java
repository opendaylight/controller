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
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class ReadyTransactionReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    public static final Class<ShardTransactionMessages.ReadyTransactionReply> SERIALIZABLE_CLASS =
            ShardTransactionMessages.ReadyTransactionReply.class;

    private String cohortPath;

    public ReadyTransactionReply() {
    }

    public ReadyTransactionReply(String cohortPath) {
        this(cohortPath, DataStoreVersions.CURRENT_VERSION);
    }

    public ReadyTransactionReply(String cohortPath, short version) {
        super(version);
        this.cohortPath = cohortPath;
    }

    public String getCohortPath() {
        return cohortPath;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        cohortPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(cohortPath);
    }

    @Override
    public Object toSerializable() {
        if(getVersion() >= DataStoreVersions.LITHIUM_VERSION) {
            return this;
        } else {
            return ShardTransactionMessages.ReadyTransactionReply.newBuilder().setActorPath(cohortPath).build();
        }
    }

    public static ReadyTransactionReply fromSerializable(Object serializable) {
        if(serializable instanceof ReadyTransactionReply) {
            return (ReadyTransactionReply)serializable;
        } else {
            ShardTransactionMessages.ReadyTransactionReply o =
                    (ShardTransactionMessages.ReadyTransactionReply) serializable;
            return new ReadyTransactionReply(o.getActorPath(), DataStoreVersions.HELIUM_2_VERSION);
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof ReadyTransactionReply ||
               message instanceof ShardTransactionMessages.ReadyTransactionReply;
    }
}
