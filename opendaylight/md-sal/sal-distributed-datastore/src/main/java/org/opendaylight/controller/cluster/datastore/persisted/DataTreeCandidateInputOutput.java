/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility serialization/deserialization for {@link DataTreeCandidate}. Note that this utility does not maintain
 * before-image information across serialization.
 */
@Beta
public final class DataTreeCandidateInputOutput {
    public record DataTreeCandidateWithVersion(
            @NonNull DataTreeCandidate candidate,
            @NonNull NormalizedNodeStreamVersion version) implements Immutable {
        public DataTreeCandidateWithVersion {
            requireNonNull(candidate);
            requireNonNull(version);
        }
    }

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

    private static DataTreeCandidateNode readModifiedNode(final ModificationType type, final NormalizedNodeDataInput in,
            final ReusableStreamReceiver receiver) throws IOException {
        final var pathArg = in.readPathArgument();
        final var children = readChildren(in, receiver);
        if (children.isEmpty()) {
            LOG.debug("Modified node {} does not have any children, not instantiating it", pathArg);
            return null;
        }

        return ModifiedDataTreeCandidateNode.create(pathArg, type, children);
    }

    private static List<DataTreeCandidateNode> readChildren(final NormalizedNodeDataInput in,
            final ReusableStreamReceiver receiver) throws IOException {
        final int size = in.readInt();
        if (size == 0) {
            return List.of();
        }

        final var ret = new ArrayList<DataTreeCandidateNode>(size);
        for (int i = 0; i < size; ++i) {
            final var child = readNode(in, receiver);
            if (child != null) {
                ret.add(child);
            }
        }
        return ret;
    }

    private static DataTreeCandidateNode readNode(final NormalizedNodeDataInput in,
            final ReusableStreamReceiver receiver) throws IOException {
        final byte type = in.readByte();
        return switch (type) {
            case APPEARED -> readModifiedNode(ModificationType.APPEARED, in, receiver);
            case DELETE -> DeletedDataTreeCandidateNode.create(in.readPathArgument());
            case DISAPPEARED -> readModifiedNode(ModificationType.DISAPPEARED, in, receiver);
            case SUBTREE_MODIFIED -> readModifiedNode(ModificationType.SUBTREE_MODIFIED, in, receiver);
            case UNMODIFIED -> null;
            case WRITE -> DataTreeCandidateNodes.written(in.readNormalizedNode(receiver));
            default -> throw new IllegalArgumentException("Unhandled node type " + type);
        };
    }

    public static DataTreeCandidateWithVersion readDataTreeCandidate(final DataInput in,
            final ReusableStreamReceiver receiver) throws IOException {
        final var reader = NormalizedNodeDataInput.newDataInput(in);
        final var rootPath = reader.readYangInstanceIdentifier();
        final byte type = reader.readByte();

        final DataTreeCandidateNode rootNode = switch (type) {
            case APPEARED -> ModifiedDataTreeCandidateNode.create(ModificationType.APPEARED,
                readChildren(reader, receiver));
            case DELETE -> DeletedDataTreeCandidateNode.create();
            case DISAPPEARED -> ModifiedDataTreeCandidateNode.create(ModificationType.DISAPPEARED,
                readChildren(reader, receiver));
            case SUBTREE_MODIFIED -> ModifiedDataTreeCandidateNode.create(ModificationType.SUBTREE_MODIFIED,
                readChildren(reader, receiver));
            case WRITE -> DataTreeCandidateNodes.written(reader.readNormalizedNode(receiver));
            case UNMODIFIED -> AbstractDataTreeCandidateNode.createUnmodified();
            default -> throw new IllegalArgumentException("Unhandled node type " + type);
        };
        return new DataTreeCandidateWithVersion(DataTreeCandidates.newDataTreeCandidate(rootPath, rootNode),
            reader.getVersion());
    }

    private static void writeChildren(final NormalizedNodeDataOutput out,
            final Collection<DataTreeCandidateNode> children) throws IOException {
        out.writeInt(children.size());
        for (var child : children) {
            writeNode(out, child);
        }
    }

    private static void writeNode(final NormalizedNodeDataOutput out, final DataTreeCandidateNode node)
            throws IOException {
        switch (node.modificationType()) {
            case APPEARED -> {
                out.writeByte(APPEARED);
                out.writePathArgument(node.name());
                writeChildren(out, node.childNodes());
            }
            case DELETE -> {
                out.writeByte(DELETE);
                out.writePathArgument(node.name());
            }
            case DISAPPEARED -> {
                out.writeByte(DISAPPEARED);
                out.writePathArgument(node.name());
                writeChildren(out, node.childNodes());
            }
            case SUBTREE_MODIFIED -> {
                out.writeByte(SUBTREE_MODIFIED);
                out.writePathArgument(node.name());
                writeChildren(out, node.childNodes());
            }
            case WRITE -> {
                out.writeByte(WRITE);
                out.writeNormalizedNode(node.getDataAfter());
            }
            case UNMODIFIED -> out.writeByte(UNMODIFIED);
            default -> throwUnhandledNodeType(node);
        }
    }

    @VisibleForTesting
    public static void writeDataTreeCandidate(final DataOutput out, final PayloadVersion version,
            final DataTreeCandidate candidate) throws IOException {
        try (var writer = version.getStreamVersion().newDataOutput(out)) {
            writer.writeYangInstanceIdentifier(candidate.getRootPath());

            final var node = candidate.getRootNode();
            switch (node.modificationType()) {
                case APPEARED -> {
                    writer.writeByte(APPEARED);
                    writeChildren(writer, node.childNodes());
                }
                case DELETE -> writer.writeByte(DELETE);
                case DISAPPEARED -> {
                    writer.writeByte(DISAPPEARED);
                    writeChildren(writer, node.childNodes());
                }
                case SUBTREE_MODIFIED -> {
                    writer.writeByte(SUBTREE_MODIFIED);
                    writeChildren(writer, node.childNodes());
                }
                case UNMODIFIED -> writer.writeByte(UNMODIFIED);
                case WRITE -> {
                    writer.writeByte(WRITE);
                    writer.writeNormalizedNode(node.getDataAfter());
                }
                default -> throwUnhandledNodeType(node);
            }
        }
    }

    public static void writeDataTreeCandidate(final DataOutput out, final DataTreeCandidate candidate)
            throws IOException {
        writeDataTreeCandidate(out, PayloadVersion.current(), candidate);
    }

    private static void throwUnhandledNodeType(final DataTreeCandidateNode node) {
        throw new IllegalArgumentException("Unhandled node type " + node.modificationType());
    }
}
