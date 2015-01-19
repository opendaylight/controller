/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

/**
 * An Externalizable version of DataChanged used for efficient serialization in lieu of protobuff.
 *
 * @author Thomas Pantelis
 */
public class ExternalizableDataChanged implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change;
    private transient short version;

    public ExternalizableDataChanged() {
        super();
    }

    public ExternalizableDataChanged(
            AsyncDataChangeEvent<YangInstanceIdentifier,NormalizedNode<?, ?>> change, short version) {
        this.change = change;
        this.version = version;
    }

    public AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> getChange() {
        return change;
    }

    public short getVersion() {
        return version;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort(); // Read the version - don't need to do anything with it now

        NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(in);

        // Note: the scope passed to builder is not actually used.
        Builder builder = DOMImmutableDataChangeEvent.builder(DataChangeScope.SUBTREE);

        // Read created data

        int size = in.readInt();
        for(int i = 0; i < size; i++) {
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            builder.addCreated(path, node);
        }

        // Read updated data

        size = in.readInt();
        for(int i = 0; i < size; i++) {
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            NormalizedNode<?, ?> before = streamReader.readNormalizedNode();
            NormalizedNode<?, ?> after = streamReader.readNormalizedNode();
            builder.addUpdated(path, before, after);
        }

        // Read removed data

        size = in.readInt();
        for(int i = 0; i < size; i++) {
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            builder.addRemoved(path, node);
        }

        // Read original subtree

        boolean present = in.readBoolean();
        if(present) {
            builder.setBefore(streamReader.readNormalizedNode());
        }

        // Read updated subtree

        present = in.readBoolean();
        if(present) {
            builder.setAfter(streamReader.readNormalizedNode());
        }

        change = builder.build();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);

        NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(out);
        NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(streamWriter);

        // Write created data

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData = change.getCreatedData();
        out.writeInt(createdData.size());
        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e: createdData.entrySet()) {
            streamWriter.writeYangInstanceIdentifier(e.getKey());
            nodeWriter.write(e.getValue());
        }

        // Write updated data

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> originalData = change.getOriginalData();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> updatedData = change.getUpdatedData();
        out.writeInt(updatedData.size());
        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e: updatedData.entrySet()) {
            streamWriter.writeYangInstanceIdentifier(e.getKey());
            nodeWriter.write(originalData.get(e.getKey()));
            nodeWriter.write(e.getValue());
        }

        // Write removed data

        Set<YangInstanceIdentifier> removed = change.getRemovedPaths();
        out.writeInt(removed.size());
        for(YangInstanceIdentifier path: removed) {
            streamWriter.writeYangInstanceIdentifier(path);
            nodeWriter.write(originalData.get(path));
        }

        // Write original subtree

        NormalizedNode<?, ?> originalSubtree = change.getOriginalSubtree();
        out.writeBoolean(originalSubtree != null);
        if(originalSubtree != null) {
            nodeWriter.write(originalSubtree);
        }

        // Write original subtree

        NormalizedNode<?, ?> updatedSubtree = change.getUpdatedSubtree();
        out.writeBoolean(updatedSubtree != null);
        if(updatedSubtree != null) {
            nodeWriter.write(updatedSubtree);
        }
    }
}
