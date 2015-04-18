/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages.AppendEntries;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;

final class DataTreeCandidatePayload extends Payload implements Externalizable {
    private static final byte DELETE = 0;
    private static final byte SUBTREE_MODIFIED = 1;
    private static final byte UNMODIFIED = 2;
    private static final byte WRITE = 3;

    private DataTreeCandidate candidate;
    private byte[] serialized;

    DataTreeCandidatePayload(final DataTreeCandidate candidate) {
        this.candidate = Preconditions.checkNotNull(candidate);
    }

    DataTreeCandidate getCandidate() {
        return candidate;
    }

    @Override
    @Deprecated
    @SuppressWarnings("rawtypes")
    public <T> Map<GeneratedExtension, T> encode() {
        return null;
    }

    @Override
    @Deprecated
    public Payload decode(final AppendEntries.ReplicatedLogEntry.Payload payload) {
        return null;
    }

    private static void writeChildren(final NormalizedNodeOutputStreamWriter writer, final DataOutput out, final Collection<DataTreeCandidateNode> children) throws IOException {
        out.writeInt(children.size());
        for (DataTreeCandidateNode child : children) {
            writeNode(writer, out, child);
        }
    }

    private static void writeNode(final NormalizedNodeOutputStreamWriter writer, final DataOutput out, final DataTreeCandidateNode node) throws IOException {
        switch (node.getModificationType()) {
        case DELETE:
            out.writeByte(DELETE);
            writer.writePathArgument(node.getIdentifier());
            break;
        case SUBTREE_MODIFIED:
            out.writeByte(SUBTREE_MODIFIED);
            writer.writePathArgument(node.getIdentifier());
            writeChildren(writer, out, node.getChildNodes());
            break;
        case UNMODIFIED:
            out.writeByte(UNMODIFIED);
            break;
        case WRITE:
            out.writeByte(WRITE);
            writer.writeNormalizedNode(node.getDataAfter().get());
            break;
        default:
            throw new IllegalArgumentException("Unhandled node type " + node.getModificationType());
        }
    }

    @Override
    public int size() {
        if (serialized == null) {
            final ByteArrayDataOutput out = ByteStreams.newDataOutput();
            try (final NormalizedNodeOutputStreamWriter writer = new NormalizedNodeOutputStreamWriter(out)) {
                writer.writeYangInstanceIdentifier(candidate.getRootPath());

                final DataTreeCandidateNode node = candidate.getRootNode();
                switch (node.getModificationType()) {
                case DELETE:
                    out.writeByte(DELETE);
                    break;
                case SUBTREE_MODIFIED:
                    writeChildren(writer, out, node.getChildNodes());
                    break;
                case UNMODIFIED:
                    out.writeByte(UNMODIFIED);
                    break;
                case WRITE:
                    out.writeByte(WRITE);
                    writer.writeNormalizedNode(node.getDataAfter().get());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled node type " + node.getModificationType());
                }

                writer.close();
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Failed to serialize candidate %s", candidate), e);
            }
            serialized = out.toByteArray();
        }

        return serialized.length;
    }

    private static Collection<DataTreeCandidateNode> readChildren(final NormalizedNodeInputStreamReader reader, final DataInput in) throws IOException {
        final int size = in.readInt();
        if (size != 0) {
            final Collection<DataTreeCandidateNode> ret = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                final DataTreeCandidateNode child = readNode(reader, in);
                if (child != null) {
                    ret.add(child);
                }
            }
            return ret;
        } else {
            return Collections.emptyList();
        }
    }

    private static DataTreeCandidateNode readNode(final NormalizedNodeInputStreamReader reader, final DataInput in) throws IOException {
        final byte type = in.readByte();
        switch (type) {
        case DELETE:
            return ChildDataTreeCandidateNode.createDeleted(reader.readPathArgument());
        case SUBTREE_MODIFIED:
            return ChildDataTreeCandidateNode.createModified(reader.readPathArgument(), readChildren(reader, in));
        case UNMODIFIED:
            return null;
        case WRITE:
            return DataTreeCandidateNodes.fromNormalizedNode(reader.readNormalizedNode());
        default:
            throw new IllegalArgumentException("Unhandled node type " + type);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final int size = in.readInt();
        serialized = new byte[size];
        in.readFully(serialized);

        final DataInput input = ByteStreams.newDataInput(serialized);
        final NormalizedNodeInputStreamReader reader = new NormalizedNodeInputStreamReader(input);
        final YangInstanceIdentifier rootPath = reader.readYangInstanceIdentifier();
        final byte type = in.readByte();
        final DataTreeCandidateNode rootNode;

        switch (type) {
        case DELETE:
            rootNode = RootDataTreeCandidateNode.createDeleted();
            break;
        case SUBTREE_MODIFIED:
            rootNode = RootDataTreeCandidateNode.createModified(readChildren(reader, in));
            break;
        case UNMODIFIED:
            rootNode = RootDataTreeCandidateNode.createUnmodified();
            break;
        case WRITE:
            rootNode = DataTreeCandidateNodes.fromNormalizedNode(reader.readNormalizedNode());
            break;
        default:
            throw new IllegalArgumentException("Unhandled node type " + type);
        }

        candidate = DataTreeCandidates.newDataTreeCandidate(rootPath, rootNode);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(size());
        out.write(serialized);
    }
}
