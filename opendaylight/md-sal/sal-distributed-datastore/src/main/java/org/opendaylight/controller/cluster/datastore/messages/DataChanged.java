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
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataChanged implements Externalizable {
    private static final long serialVersionUID = 1L;

    private AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change;

    public DataChanged() {
    }

    public DataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.change = change;
    }

    public AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> getChange() {
        return change;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        in.readShort(); // Read the version

        NormalizedNodeDataInput streamReader = new NormalizedNodeInputStreamReader(in);

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
        out.writeShort(DataStoreVersions.CURRENT_VERSION);

        NormalizedNodeDataOutput streamWriter = NormalizedNodeInputOutput.newDataOutput(out);

        // Write created data

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData = change.getCreatedData();
        out.writeInt(createdData.size());
        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e: createdData.entrySet()) {
            streamWriter.writeYangInstanceIdentifier(e.getKey());
            streamWriter.writeNormalizedNode(e.getValue());
        }

        // Write updated data

        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> originalData = change.getOriginalData();
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> updatedData = change.getUpdatedData();
        out.writeInt(updatedData.size());
        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> e: updatedData.entrySet()) {
            streamWriter.writeYangInstanceIdentifier(e.getKey());
            streamWriter.writeNormalizedNode(originalData.get(e.getKey()));
            streamWriter.writeNormalizedNode(e.getValue());
        }

        // Write removed data

        Set<YangInstanceIdentifier> removed = change.getRemovedPaths();
        out.writeInt(removed.size());
        for(YangInstanceIdentifier path: removed) {
            streamWriter.writeYangInstanceIdentifier(path);
            streamWriter.writeNormalizedNode(originalData.get(path));
        }

        // Write original subtree

        NormalizedNode<?, ?> originalSubtree = change.getOriginalSubtree();
        out.writeBoolean(originalSubtree != null);
        if(originalSubtree != null) {
            streamWriter.writeNormalizedNode(originalSubtree);
        }

        // Write original subtree

        NormalizedNode<?, ?> updatedSubtree = change.getUpdatedSubtree();
        out.writeBoolean(updatedSubtree != null);
        if(updatedSubtree != null) {
            streamWriter.writeNormalizedNode(updatedSubtree);
        }
    }
}
