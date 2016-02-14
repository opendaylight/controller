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

public class DataExistsReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    @Deprecated
    private static final ShardTransactionMessages.DataExistsReply SERIALIZABLE_TRUE =
            ShardTransactionMessages.DataExistsReply.newBuilder().setExists(true).build();
    @Deprecated
    private static final ShardTransactionMessages.DataExistsReply SERIALIZABLE_FALSE =
            ShardTransactionMessages.DataExistsReply.newBuilder().setExists(false).build();

    private boolean exists;

    public DataExistsReply() {
    }

    public DataExistsReply(final boolean exists, final short version) {
        super(version);
        this.exists = exists;
    }

    public boolean exists() {
        return exists;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        exists = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeBoolean(exists);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return exists ? SERIALIZABLE_TRUE : SERIALIZABLE_FALSE;
    }

    public static DataExistsReply fromSerializable(final Object serializable) {
        if(serializable instanceof DataExistsReply) {
            return (DataExistsReply)serializable;
        } else {
            ShardTransactionMessages.DataExistsReply o = (ShardTransactionMessages.DataExistsReply) serializable;
            return new DataExistsReply(o.getExists(), DataStoreVersions.LITHIUM_VERSION);
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof DataExistsReply || message instanceof ShardTransactionMessages.DataExistsReply;
    }
}
