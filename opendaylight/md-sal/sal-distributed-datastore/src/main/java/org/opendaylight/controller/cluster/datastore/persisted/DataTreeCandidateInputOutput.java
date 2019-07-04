/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.ReusableImmutableNormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility serialization/deserialization for {@link DataTreeCandidate}. Note that this utility does not maintain
 * before-image information across serialization.
 *
 * @author Robert Varga
 */
@Beta
public final class DataTreeCandidateInputOutput {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeCandidateInputOutput.class);
    private static final byte DELETE = 0;
    private static final byte SUBTREE_MODIFIED = 1;
    private static final byte UNMODIFIED = 2;
    private static final byte WRITE = 3;
    private static final byte APPEARED = 4;
    private static final byte DISAPPEARED = 5;

    private DataTreeCandidateInputOutput() {
        throw new UnsupportedOperationException();
    }

    private static DataTreeCandidateNode readModifiedNode(final ModificationType type,
            final NormalizedNodeDataInput in, final ReusableImmutableNormalizedNodeStreamWriter writer)
                    throws IOException {

        final PathArgument identifier = in.readPathArgument();
        final Collection<DataTreeCandidateNode> children = readChildren(in, writer);
        if (children.isEmpty()) {
            LOG.debug("Modified node {} does not have any children, not instantiating it", identifier);
            return null;
        }

        return ModifiedDataTreeCandidateNode.create(identifier, type, children);
    }

    private static Collection<DataTreeCandidateNode> readChildren(final NormalizedNodeDataInput in,
            final ReusableImmutableNormalizedNodeStreamWriter writer) throws IOException {
        final int size = in.readInt();
        if (size == 0) {
            return ImmutableList.of();
        }

        final Collection<DataTreeCandidateNode> ret = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final DataTreeCandidateNode child = readNode(in, writer);
            if (child != null) {
                ret.add(child);
            }
        }
        return ret;
    }

    private static DataTreeCandidateNode readNode(final NormalizedNodeDataInput in,
            final ReusableImmutableNormalizedNodeStreamWriter writer) throws IOException {
        final byte type = in.readByte();
        switch (type) {
            case APPEARED:
                return readModifiedNode(ModificationType.APPEARED, in, writer);
            case DELETE:
                return DeletedDataTreeCandidateNode.create(in.readPathArgument());
            case DISAPPEARED:
                return readModifiedNode(ModificationType.DISAPPEARED, in, writer);
            case SUBTREE_MODIFIED:
                return readModifiedNode(ModificationType.SUBTREE_MODIFIED, in, writer);
            case UNMODIFIED:
                return null;
            case WRITE:
                return DataTreeCandidateNodes.written(in.readNormalizedNode(writer));
            default:
                throw new IllegalArgumentException("Unhandled node type " + type);
        }
    }

    public static DataTreeCandidate readDataTreeCandidate(final DataInput in,
            final ReusableImmutableNormalizedNodeStreamWriter writer) throws IOException {
        final NormalizedNodeDataInput reader = NormalizedNodeInputOutput.newDataInput(in);
        final YangInstanceIdentifier rootPath = reader.readYangInstanceIdentifier();
        final byte type = reader.readByte();

        final DataTreeCandidateNode rootNode;
        switch (type) {
            case APPEARED:
                rootNode = ModifiedDataTreeCandidateNode.create(ModificationType.APPEARED,
                    readChildren(reader, writer));
                break;
            case DELETE:
                rootNode = DeletedDataTreeCandidateNode.create();
                break;
            case DISAPPEARED:
                rootNode = ModifiedDataTreeCandidateNode.create(ModificationType.DISAPPEARED,
                    readChildren(reader, writer));
                break;
            case SUBTREE_MODIFIED:
                rootNode = ModifiedDataTreeCandidateNode.create(ModificationType.SUBTREE_MODIFIED,
                        readChildren(reader, writer));
                break;
            case WRITE:
                rootNode = DataTreeCandidateNodes.written(reader.readNormalizedNode(writer));
                break;
            case UNMODIFIED:
                rootNode = AbstractDataTreeCandidateNode.createUnmodified();
                break;
            default:
                throw new IllegalArgumentException("Unhandled node type " + type);
        }

        return DataTreeCandidates.newDataTreeCandidate(rootPath, rootNode);
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
                throwUnhandledNodeType(node);
        }
    }

    public static void writeDataTreeCandidate(final DataOutput out, final DataTreeCandidate candidate)
            throws IOException {
        try (NormalizedNodeDataOutput writer = NormalizedNodeInputOutput.newDataOutput(out,
                PayloadVersion.current().getStreamVersion())) {
            writer.writeYangInstanceIdentifier(candidate.getRootPath());

            final DataTreeCandidateNode node = candidate.getRootNode();
            switch (node.getModificationType()) {
                case APPEARED:
                    writer.writeByte(APPEARED);
                    writeChildren(writer, node.getChildNodes());
                    break;
                case DELETE:
                    writer.writeByte(DELETE);
                    break;
                case DISAPPEARED:
                    writer.writeByte(DISAPPEARED);
                    writeChildren(writer, node.getChildNodes());
                    break;
                case SUBTREE_MODIFIED:
                    writer.writeByte(SUBTREE_MODIFIED);
                    writeChildren(writer, node.getChildNodes());
                    break;
                case UNMODIFIED:
                    writer.writeByte(UNMODIFIED);
                    break;
                case WRITE:
                    writer.writeByte(WRITE);
                    writer.writeNormalizedNode(node.getDataAfter().get());
                    break;
                default:
                    throwUnhandledNodeType(node);
            }
        }
    }

    private static void throwUnhandledNodeType(final DataTreeCandidateNode node) {
        throw new IllegalArgumentException("Unhandled node type " + node.getModificationType());
    }
}
