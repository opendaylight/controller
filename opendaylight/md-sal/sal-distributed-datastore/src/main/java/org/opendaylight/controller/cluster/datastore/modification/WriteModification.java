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
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;

/**
 * WriteModification stores all the parameters required to write data to the specified path.
 */
public class WriteModification extends AbstractModification {
    private static final long serialVersionUID = 1L;

    private NormalizedNode<?, ?> data;

    public WriteModification() {
        this(DataStoreVersions.CURRENT_VERSION);
    }

    public WriteModification(final short version) {
        super(version);
    }

    WriteModification(final short version, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        super(version, path);
        this.data = data;
    }

    public WriteModification(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        super(path);
        this.data = data;
    }

    @Override
    public void apply(final DOMStoreWriteTransaction transaction) {
        transaction.write(getPath(), data);
    }

    @Override
    public void apply(final DataTreeModification transaction) {
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
    public void readExternal(final ObjectInput in) throws IOException {
        SerializationUtils.readNodeAndPath(in, this, (instance, path, node) -> {
            instance.setPath(path);
            instance.data = node;
        });
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        SerializationUtils.writeNodeAndPath(out, getPath(), data);
    }

    public static WriteModification fromStream(final NormalizedNodeDataInput in, final short version,
            final ReusableStreamReceiver receiver) throws IOException {
        final NormalizedNode<?, ?> node = in.readNormalizedNode(receiver);
        final YangInstanceIdentifier path = in.readYangInstanceIdentifier();
        return new WriteModification(version, path, node);
    }

    @Override
    public void writeTo(final NormalizedNodeDataOutput out) throws IOException {
        // FIXME: this should be inverted, as the path helps receivers in establishment of context
        out.writeNormalizedNode(data);
        out.writeYangInstanceIdentifier(getPath());
    }
}
