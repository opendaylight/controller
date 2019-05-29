/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Provides various utility methods for serialization and de-serialization.
 *
 * @author Thomas Pantelis
 */
public final class SerializationUtils {
    @Deprecated
    public static final ThreadLocal<NormalizedNodeDataOutput> REUSABLE_WRITER_TL = new ThreadLocal<>();
    @Deprecated
    public static final ThreadLocal<NormalizedNodeDataInput> REUSABLE_READER_TL = new ThreadLocal<>();

    private SerializationUtils() {

    }

    @FunctionalInterface
    public interface Applier<T> {
        void apply(T instance, YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    @Deprecated
    private static NormalizedNodeDataOutput streamWriter(final DataOutput out) {
        NormalizedNodeDataOutput streamWriter = REUSABLE_WRITER_TL.get();
        if (streamWriter == null) {
            streamWriter = NormalizedNodeInputOutput.newDataOutput(out);
        }

        return streamWriter;
    }

    @Deprecated
    private static NormalizedNodeDataInput streamReader(final DataInput in) throws IOException {
        NormalizedNodeDataInput streamReader = REUSABLE_READER_TL.get();
        if (streamReader == null) {
            streamReader = NormalizedNodeInputOutput.newDataInput(in);
        }

        return streamReader;
    }

    @Deprecated
    public static void serializePathAndNode(final YangInstanceIdentifier path, final NormalizedNode<?, ?> node,
            final DataOutput out) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(node);
        try {
            NormalizedNodeDataOutput streamWriter = streamWriter(out);
            streamWriter.writeNormalizedNode(node);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path %s and Node %s", path, node), e);
        }
    }

    @Deprecated
    public static <T> void deserializePathAndNode(final DataInput in, final T instance, final Applier<T> applier) {
        try {
            NormalizedNodeDataInput streamReader = streamReader(in);
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            applier.apply(instance, path, node);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path and Node", e);
        }
    }

    private static NormalizedNode<?, ?> tryDeserializeNormalizedNode(final DataInput in) throws IOException {
        boolean present = in.readBoolean();
        if (present) {
            NormalizedNodeDataInput streamReader = streamReader(in);
            return streamReader.readNormalizedNode();
        }

        return null;
    }

    @Deprecated
    public static NormalizedNode<?, ?> deserializeNormalizedNode(final DataInput in) {
        try {
            return tryDeserializeNormalizedNode(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
        }
    }

    @Deprecated
    public static NormalizedNode<?, ?> deserializeNormalizedNode(final byte [] bytes) {
        try {
            return tryDeserializeNormalizedNode(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
        }
    }

    public static Optional<NormalizedNode<?, ?>> readNormalizedNode(final DataInput in) throws IOException {
        if (!in.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(NormalizedNodeInputOutput.newDataInput(in).readNormalizedNode());
    }

    @Deprecated
    public static void serializeNormalizedNode(final NormalizedNode<?, ?> node, final DataOutput out) {
        try {
            out.writeBoolean(node != null);
            if (node != null) {
                NormalizedNodeDataOutput streamWriter = streamWriter(out);
                streamWriter.writeNormalizedNode(node);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing NormalizedNode %s", node), e);
        }
    }

    public static byte [] serializeNormalizedNode(final NormalizedNode<?, ?> node) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serializeNormalizedNode(node, new DataOutputStream(bos));
        return bos.toByteArray();
    }

    public static void writeNormalizedNode(final DataOutput out, final @Nullable NormalizedNode<?, ?> node)
            throws IOException {
        if (node != null) {
            out.writeBoolean(true);

            try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
                stream.writeNormalizedNode(node);
            }
        } else {
            out.writeBoolean(false);
        }
    }

    @Deprecated
    public static void serializePath(final YangInstanceIdentifier path, final DataOutput out) {
        Preconditions.checkNotNull(path);
        try {
            NormalizedNodeDataOutput streamWriter = streamWriter(out);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path %s", path), e);
        }
    }

    public static void writeNormalizedNode(final DataOutput out, final NormalizedNodeStreamVersion version,
            final @Nullable NormalizedNode<?, ?> node) throws IOException {
        if (node != null) {
            out.writeBoolean(true);

            try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out, version)) {
                stream.writeNormalizedNode(node);
            }
        } else {
            out.writeBoolean(false);
        }
    }

    public static YangInstanceIdentifier readPath(final DataInput in) throws IOException {
        return NormalizedNodeInputOutput.newDataInput(in).readYangInstanceIdentifier();
    }

    public static void writePath(final DataOutput out, final @NonNull YangInstanceIdentifier path)
            throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
            stream.writeYangInstanceIdentifier(path);
        }
    }

    public static void writePath(final DataOutput out, final NormalizedNodeStreamVersion version,
            final @NonNull YangInstanceIdentifier path) throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out, version)) {
            stream.writeYangInstanceIdentifier(path);
        }
    }

    public static <T> void readNodeAndPath(final DataInput in, final T instance, final Applier<T> applier)
            throws IOException {
        final NormalizedNodeDataInput stream = NormalizedNodeInputOutput.newDataInput(in);
        NormalizedNode<?, ?> node = stream.readNormalizedNode();
        YangInstanceIdentifier path = stream.readYangInstanceIdentifier();
        applier.apply(instance, path, node);
    }

    public static void writeNodeAndPath(final DataOutput out, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> node) throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
            stream.writeNormalizedNode(node);
            stream.writeYangInstanceIdentifier(path);
        }
    }

    public static <T> void readPathAndNode(final DataInput in, final T instance, final Applier<T> applier)
            throws IOException {
        final NormalizedNodeDataInput stream = NormalizedNodeInputOutput.newDataInput(in);
        YangInstanceIdentifier path = stream.readYangInstanceIdentifier();
        NormalizedNode<?, ?> node = stream.readNormalizedNode();
        applier.apply(instance, path, node);
    }

    public static void writePathAndNode(final DataOutput out, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> node) throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
            stream.writeYangInstanceIdentifier(path);
            stream.writeNormalizedNode(node);
        }
    }

    @Deprecated
    public static YangInstanceIdentifier deserializePath(final DataInput in) {
        try {
            NormalizedNodeDataInput streamReader = streamReader(in);
            return streamReader.readYangInstanceIdentifier();
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path", e);
        }
    }
}
