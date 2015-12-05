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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.DictionaryNormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.DictionaryNormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputDictionary;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputDictionary;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages.AppendEntries;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataTreeCandidatePayload extends Payload implements Externalizable {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCandidatePayload.class);
    private static final long serialVersionUID = 1L;
    private static final byte DELETE = 0;
    private static final byte SUBTREE_MODIFIED = 1;
    private static final byte UNMODIFIED = 2;
    private static final byte WRITE = 3;
    private static final byte APPEARED = 4;
    private static final byte DISAPPEARED = 5;

    private transient byte[] serialized;

    public DataTreeCandidatePayload() {
        // Required by Externalizable
    }

    private DataTreeCandidatePayload(final byte[] serialized) {
        this.serialized = Preconditions.checkNotNull(serialized);
    }

    private static void writeChildren(final NormalizedNodeDataOutput out,
            final Collection<DataTreeCandidateNode> children) throws IOException {
        out.writeInt(children.size());
        for (DataTreeCandidateNode child : children) {
            writeNode(out, child);
        }
    }

    private static void writeNode(final NormalizedNodeDataOutput out, final DataTreeCandidateNode node)
            throws IOException {
        switch (node.getModificationType()) {
        case APPEARED:
            out.writeByte(APPEARED);
            out.writePathArgument(node.getIdentifier());
            writeChildren(out, node.getChildNodes());
            break;
        case DELETE:
            out.writeByte(DELETE);
            out.writePathArgument(node.getIdentifier());
            break;
        case DISAPPEARED:
            out.writeByte(DISAPPEARED);
            out.writePathArgument(node.getIdentifier());
            writeChildren(out, node.getChildNodes());
            break;
        case SUBTREE_MODIFIED:
            out.writeByte(SUBTREE_MODIFIED);
            out.writePathArgument(node.getIdentifier());
            writeChildren(out, node.getChildNodes());
            break;
        case WRITE:
            out.writeByte(WRITE);
            out.writeNormalizedNode(node.getDataAfter().get());
            break;
        case UNMODIFIED:
            out.writeByte(UNMODIFIED);
            break;
        default:
            throw new IllegalArgumentException("Unhandled node type " + node.getModificationType());
        }
    }

    static DataTreeCandidatePayload create(final DataTreeCandidate candidate) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try (final NormalizedNodeOutputStreamWriter writer = new NormalizedNodeOutputStreamWriter(out)) {
            serializeCandidate(writer, candidate);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to serialize candidate %s", candidate), e);
        }

        return new DataTreeCandidatePayload(out.toByteArray());
    }

    static Payload create(final DataTreeCandidate candidate, final NormalizedNodeOutputDictionary dictionary) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        final DictionaryNormalizedNodeDataOutput writer;

        try {
            writer = NormalizedNodeInputOutput.newDictionaryDataOutput(out, dictionary);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to instantiate output writer", e);
        }

        try {
            serializeCandidate(writer, candidate);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to serialize candidate %s", candidate), e);
        } finally {
            writer.detachDictionary();
        }

        return new DataTreeCandidatePayload(out.toByteArray());
    }

    private static void serializeCandidate(final NormalizedNodeDataOutput out, final DataTreeCandidate candidate)
            throws IOException {
        out.writeYangInstanceIdentifier(candidate.getRootPath());

        final DataTreeCandidateNode node = candidate.getRootNode();
        switch (node.getModificationType()) {
        case APPEARED:
            out.writeByte(APPEARED);
            writeChildren(out, node.getChildNodes());
            break;
        case DELETE:
            out.writeByte(DELETE);
            break;
        case DISAPPEARED:
            out.writeByte(DISAPPEARED);
            writeChildren(out, node.getChildNodes());
            break;
        case SUBTREE_MODIFIED:
            out.writeByte(SUBTREE_MODIFIED);
            writeChildren(out, node.getChildNodes());
            break;
        case UNMODIFIED:
            out.writeByte(UNMODIFIED);
            break;
        case WRITE:
            out.writeByte(WRITE);
            out.writeNormalizedNode(node.getDataAfter().get());
            break;
        default:
            throw new IllegalArgumentException("Unhandled node type " + node.getModificationType());
        }
    }

    private static Collection<DataTreeCandidateNode> readChildren(final NormalizedNodeDataInput in) throws IOException {
        final int size = in.readInt();
        if (size != 0) {
            final Collection<DataTreeCandidateNode> ret = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                final DataTreeCandidateNode child = readNode(in);
                if (child != null) {
                    ret.add(child);
                }
            }
            return ret;
        } else {
            return Collections.emptyList();
        }
    }

    private static DataTreeCandidateNode readModifiedNode(final ModificationType type,
            final NormalizedNodeDataInput in) throws IOException {

        final PathArgument identifier = in.readPathArgument();
        final Collection<DataTreeCandidateNode> children = readChildren(in);
        if (children.isEmpty()) {
            LOG.debug("Modified node {} does not have any children, not instantiating it", identifier);
            return null;
        } else {
            return ModifiedDataTreeCandidateNode.create(identifier, type, children);
        }
    }

    private static DataTreeCandidateNode readNode(final NormalizedNodeDataInput in) throws IOException {
        final byte type = in.readByte();
        switch (type) {
        case APPEARED:
            return readModifiedNode(ModificationType.APPEARED, in);
        case DELETE:
            return DeletedDataTreeCandidateNode.create(in.readPathArgument());
        case DISAPPEARED:
            return readModifiedNode(ModificationType.DISAPPEARED, in);
        case SUBTREE_MODIFIED:
            return readModifiedNode(ModificationType.SUBTREE_MODIFIED, in);
        case UNMODIFIED:
            return null;
        case WRITE:
            return DataTreeCandidateNodes.fromNormalizedNode(in.readNormalizedNode());
        default:
            throw new IllegalArgumentException("Unhandled node type " + type);
        }
    }

    private static DataTreeCandidate parseCandidate(final NormalizedNodeDataInput in) throws IOException {
        final YangInstanceIdentifier rootPath = in.readYangInstanceIdentifier();
        final byte type = in.readByte();

        final DataTreeCandidateNode rootNode;
        switch (type) {
        case DELETE:
            rootNode = DeletedDataTreeCandidateNode.create();
            break;
        case SUBTREE_MODIFIED:
            rootNode = ModifiedDataTreeCandidateNode.create(readChildren(in));
            break;
        case WRITE:
            rootNode = DataTreeCandidateNodes.fromNormalizedNode(in.readNormalizedNode());
            break;
        default:
            throw new IllegalArgumentException("Unhandled node type " + type);
        }

        return DataTreeCandidates.newDataTreeCandidate(rootPath, rootNode);
    }

    DataTreeCandidate getCandidate() throws IOException {
        final DataInput input = ByteStreams.newDataInput(serialized);

        return parseCandidate(NormalizedNodeInputOutput.newDataInput(input));
    }

    Entry<DataTreeCandidate, NormalizedNodeInputDictionary> getCandidate(@Nullable final NormalizedNodeInputDictionary dictionary)
            throws IOException {
        final DataInput input = ByteStreams.newDataInput(serialized);
        final DictionaryNormalizedNodeDataInput reader = NormalizedNodeInputOutput.newDictionaryDataInput(input,
            dictionary);

        final DataTreeCandidate candidate = parseCandidate(reader);
        return new SimpleImmutableEntry<>(candidate, reader.detachDictionary());
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

    @Override
    public int size() {
        return serialized.length;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeByte((byte)serialVersionUID);
        out.writeInt(serialized.length);
        out.write(serialized);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final long version = in.readByte();
        Preconditions.checkArgument(version == serialVersionUID, "Unsupported serialization version %s", version);

        final int length = in.readInt();
        serialized = new byte[length];
        in.readFully(serialized);
    }
}
