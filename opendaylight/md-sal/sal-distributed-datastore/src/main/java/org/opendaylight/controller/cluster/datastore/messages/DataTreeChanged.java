/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

public final class DataTreeChanged implements Externalizable {
    private static final long serialVersionUID = 1L;
    // Java serialization APIs mean that we could make this field final
    // at the cost of using arcane reflections in readExternal().
    private Collection<DataTreeCandidate> changes;

    public DataTreeChanged(final Collection<DataTreeCandidate> changes) {
        this.changes = Preconditions.checkNotNull(changes);
    }

    /**
     * Return the data changes. Note that these may not be semantically complete if
     * this object was deserialized. Specifically the afterData() information will
     * be missing if the change contained any MERGE operations. Users MUST perform
     * data reconstruction.
     *
     * @return Change events
     */
    public Collection<DataTreeCandidate> getChanges() {
        return changes;
    }

    private static void writeChildren(final ObjectOutput out, final Collection<DataTreeCandidateNode> children) throws IOException {
        out.writeInt(children.size());
        for (DataTreeCandidateNode child : children) {
            writeNode(out, child);
        }
    }

    private static void writeData(final ObjectOutput out, final NormalizedNode<?, ?> data) {
        // FIXME: implement this
    }

    private static DataTreeCandidateNode readNode(final ObjectInput in) throws ClassNotFoundException, IOException {
        final ModificationType type = (ModificationType) in.readObject();

        final PathArgument id;

        switch (type) {
        case UNMODIFIED:
            return null;
        case DELETE:
            id = (PathArgument) in.readObject();
            return new DataTreeCandidateNodeImpl(id);
        case MERGE:
            break;
        case SUBTREE_MODIFIED:
            break;
        case WRITE:
            break;
        default:
            throw new IllegalArgumentException("Unhandled modification type " + type);
        }

        // FIXME: implement this
        return null;
    }

    private static void writeNode(final ObjectOutput out, final DataTreeCandidateNode node) throws IOException {
        out.writeObject(node.getModificationType());

        final ModificationType type = node.getModificationType();
        if (type == ModificationType.UNMODIFIED) {
            // No-op, but we had to write the type, as that accounts for this node
            return;
        }

        out.writeObject(node.getIdentifier());

        switch (node.getModificationType()) {
        case UNMODIFIED:
            // Taken care of above
            break;
        case DELETE:
            // Nothing more to do
            break;
        case WRITE:
            // Just write the after-image
            writeData(out, node.getDataAfter().get());
            break;
        case MERGE:
            /*
             * This is something that we can optimize. A merge builds on top
             * of previous data. We do not want to transfer the after image
             * completely, as it may be huge (as it contains all the data).
             *
             * Take a peek at beforeData and our children. If we do not have
             * children, transmit as a write (as that means an empty container
             * or a leaf). If there was no beforeData, also transmit as a write.
             *
             * If it's none of those cases, this is a full-blown merge, but we
             * know what new data was overwritten by examining children. Transmit
             * only that and the receiver will reconstruct it.
             */
            if (!node.getDataBefore().isPresent() || node.getChildNodes().isEmpty()) {
                out.writeBoolean(true);
                writeData(out, node.getDataAfter().get());
            } else {
                out.writeBoolean(false);
                writeChildren(out, node.getChildNodes());
            }
            break;
        case SUBTREE_MODIFIED:
            // Write out children only
            writeChildren(out, node.getChildNodes());
            break;
        }
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(changes.size());

        for (DataTreeCandidate change : changes) {
            out.writeObject(change.getRootPath());
            writeNode(out, change.getRootNode());
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int size = in.readInt();
        if (size == 0) {
            changes = Collections.emptyList();
            return;
        }

        changes = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final YangInstanceIdentifier treeId = (YangInstanceIdentifier) in.readObject();
            final DataTreeCandidateNode node = readNode(in);

            // Null means suppressed node
            if (node != null) {
                changes.add(new DataTreeCandidateImpl(treeId, node));
            }
        }
    }

    private static final class DataTreeCandidateImpl implements DataTreeCandidate {
        private final YangInstanceIdentifier treeId;
        private final DataTreeCandidateNode node;

        public DataTreeCandidateImpl(final YangInstanceIdentifier treeId, final DataTreeCandidateNode node) {
            this.treeId = Preconditions.checkNotNull(treeId);
            this.node = Preconditions.checkNotNull(node);
        }

        @Override
        public DataTreeCandidateNode getRootNode() {
            return node;
        }

        @Override
        public YangInstanceIdentifier getRootPath() {
            return treeId;
        }
    }
}
