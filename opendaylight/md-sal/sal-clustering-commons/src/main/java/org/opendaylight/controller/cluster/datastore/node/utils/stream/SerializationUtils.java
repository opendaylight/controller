/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion.MAGNESIUM;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;

/**
 * Provides various utility methods for serialization and de-serialization.
 *
 * @author Thomas Pantelis
 */
public final class SerializationUtils {
    private SerializationUtils() {

    }

    @FunctionalInterface
    public interface Applier<T> {
        void apply(T instance, YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    public static Optional<NormalizedNode<?, ?>> readNormalizedNode(final DataInput in) throws IOException {
        if (!in.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(NormalizedNodeDataInput.newDataInput(in).readNormalizedNode());
    }

    public static void writeNormalizedNode(final DataOutput out, final @Nullable NormalizedNode<?, ?> node)
            throws IOException {
        writeNormalizedNode(out, MAGNESIUM, node);
    }

    public static void writeNormalizedNode(final DataOutput out,
            final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion version,
            final @Nullable NormalizedNode<?, ?> node) throws IOException {
        if (node != null) {
            out.writeBoolean(true);

            try (NormalizedNodeDataOutput stream = version.newDataOutput(out)) {
                stream.writeNormalizedNode(node);
            }
        } else {
            out.writeBoolean(false);
        }
    }

    @Deprecated(forRemoval = true)
    public static void writeNormalizedNode(final DataOutput out, final NormalizedNodeStreamVersion version,
            final @Nullable NormalizedNode<?, ?> node) throws IOException {
        writeNormalizedNode(out, version.toYangtools(), node);
    }

    public static YangInstanceIdentifier readPath(final DataInput in) throws IOException {
        return NormalizedNodeDataInput.newDataInput(in).readYangInstanceIdentifier();
    }

    public static void writePath(final DataOutput out, final @NonNull YangInstanceIdentifier path)
            throws IOException {
        writePath(out, MAGNESIUM, path);
    }

    public static void writePath(final DataOutput out,
            final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion version,
            final @NonNull YangInstanceIdentifier path) throws IOException {
        try (NormalizedNodeDataOutput stream = version.newDataOutput(out)) {
            stream.writeYangInstanceIdentifier(path);
        }
    }

    @Deprecated(forRemoval = true)
    public static void writePath(final DataOutput out, final NormalizedNodeStreamVersion version,
            final @NonNull YangInstanceIdentifier path) throws IOException {
        writePath(out, version.toYangtools(), path);
    }

    public static <T> void readNodeAndPath(final DataInput in, final T instance, final Applier<T> applier)
            throws IOException {
        final NormalizedNodeDataInput stream = NormalizedNodeDataInput.newDataInput(in);
        NormalizedNode<?, ?> node = stream.readNormalizedNode();
        YangInstanceIdentifier path = stream.readYangInstanceIdentifier();
        applier.apply(instance, path, node);
    }

    public static void writeNodeAndPath(final DataOutput out,
            final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion version,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> node) throws IOException {
        try (NormalizedNodeDataOutput stream = version.newDataOutput(out)) {
            stream.writeNormalizedNode(node);
            stream.writeYangInstanceIdentifier(path);
        }
    }

    public static void writeNodeAndPath(final DataOutput out, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> node) throws IOException {
        writeNodeAndPath(out, MAGNESIUM, path, node);
    }

    public static <T> void readPathAndNode(final DataInput in, final T instance, final Applier<T> applier)
            throws IOException {
        final NormalizedNodeDataInput stream = NormalizedNodeDataInput.newDataInput(in);
        YangInstanceIdentifier path = stream.readYangInstanceIdentifier();
        NormalizedNode<?, ?> node = stream.readNormalizedNode();
        applier.apply(instance, path, node);
    }

    public static void writePathAndNode(final DataOutput out,
            final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion version,
            final YangInstanceIdentifier path, final NormalizedNode<?, ?> node) throws IOException {
        try (NormalizedNodeDataOutput stream = version.newDataOutput(out)) {
            stream.writeYangInstanceIdentifier(path);
            stream.writeNormalizedNode(node);
        }
    }

    public static void writePathAndNode(final DataOutput out, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> node) throws IOException {
        writePathAndNode(out, MAGNESIUM, path, node);
    }
}
