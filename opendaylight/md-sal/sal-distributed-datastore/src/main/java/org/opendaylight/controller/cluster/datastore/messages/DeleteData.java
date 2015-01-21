/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class DeleteData implements VersionedSerializableMessage, Externalizable {
    private static final long serialVersionUID = 1L;

    public static final Class<DeleteData> SERIALIZABLE_CLASS = DeleteData.class;

    private YangInstanceIdentifier path;
    private short version;

    public DeleteData() {
    }

    public DeleteData(final YangInstanceIdentifier path) {
        this.path = path;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public short getVersion() {
        return version;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort(); // Read the version - don't need to do anything with it now
        path = SerializationUtils.deserializePath(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);
        SerializationUtils.serializePath(path, out);
    }

    @Override
    public Object toSerializable(short toVersion) {
        if(toVersion >= DataStoreVersions.LITHIUM_VERSION) {
            version = toVersion;
            return this;
        } else {
            // To base or R1 Helium version
            return ShardTransactionMessages.DeleteData.newBuilder().setInstanceIdentifierPathArguments(
                    InstanceIdentifierUtils.toSerializable(path)).build();
        }
    }

    public static DeleteData fromSerializable(final Object serializable) {
        if(serializable instanceof DeleteData) {
            return (DeleteData) serializable;
        } else {
            // From base or R1 Helium version
            ShardTransactionMessages.DeleteData o = (ShardTransactionMessages.DeleteData) serializable;
            return new DeleteData(InstanceIdentifierUtils.fromSerializable(o.getInstanceIdentifierPathArguments()));
        }
    }

    public static boolean isSerializedType(Object message) {
        return SERIALIZABLE_CLASS.isInstance(message) ||
               message instanceof ShardTransactionMessages.DeleteData;
    }
}
