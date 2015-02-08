/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Decoded;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Encoded;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils.Applier;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * WriteModification stores all the parameters required to write data to the specified path
 */
public class WriteModification extends AbstractModification {
    private static final long serialVersionUID = 1L;

    private NormalizedNode<?, ?> data;

    public WriteModification() {
        this(DataStoreVersions.CURRENT_VERSION);
    }

    public WriteModification(short version) {
        super(version);
    }

    public WriteModification(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        super(path);
        this.data = data;
    }

    @Override
    public void apply(final DOMStoreWriteTransaction transaction) {
        transaction.write(getPath(), data);
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    @Override
    public byte getType() {
        return WRITE;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        SerializationUtils.deserializePathAndNode(in, this, APPLIER);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializationUtils.serializePathAndNode(getPath(), data, out);
    }

    @Override
    @Deprecated
    public Object toSerializable() {
        Encoded encoded = new NormalizedNodeToNodeCodec(null).encode(getPath(), getData());
        return PersistentMessages.Modification.newBuilder().setType(this.getClass().toString())
                .setPath(encoded.getEncodedPath()).setData(encoded.getEncodedNode()
                        .getNormalizedNode()).build();
    }

    @Deprecated
    public static WriteModification fromSerializable(final Object serializable) {
        PersistentMessages.Modification o = (PersistentMessages.Modification) serializable;
        Decoded decoded = new NormalizedNodeToNodeCodec(null).decode(o.getPath(), o.getData());
        return new WriteModification(decoded.getDecodedPath(), decoded.getDecodedNode());
    }

    public static WriteModification fromStream(ObjectInput in, short version)
            throws ClassNotFoundException, IOException {
        WriteModification mod = new WriteModification(version);
        mod.readExternal(in);
        return mod;
    }

    private static final Applier<WriteModification> APPLIER = new Applier<WriteModification>() {
        @Override
        public void apply(WriteModification instance, YangInstanceIdentifier path,
                NormalizedNode<?, ?> node) {
            instance.setPath(path);
            instance.data = node;
        }
    };
}
