/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
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
    public static final ThreadLocal<NormalizedNodeDataInput> REUSABLE_READER_TL = new ThreadLocal<>();

    private SerializationUtils() {
    }

    public interface Applier<T> {
        void apply(T instance, YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    private static NormalizedNodeDataInput streamReader(final DataInput in) throws IOException {
        NormalizedNodeDataInput streamReader = REUSABLE_READER_TL.get();
        if (streamReader == null) {
            streamReader = NormalizedNodeInputOutput.newDataInput(in);
        }

        return streamReader;
    }

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

    public static NormalizedNode<?, ?> deserializeNormalizedNode(final DataInput in) {
        try {
            return tryDeserializeNormalizedNode(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
        }
    }

    public static NormalizedNode<?, ?> deserializeNormalizedNode(final byte [] bytes) {
        try {
            return tryDeserializeNormalizedNode(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
        }
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

    public static void writePath(final DataOutput out, final @NonNull YangInstanceIdentifier path)
            throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
            stream.writeYangInstanceIdentifier(path);
        }
    }

    public static void writeNodeAndPath(final DataOutput out, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> node) throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
            stream.writeNormalizedNode(node);
            stream.writeYangInstanceIdentifier(path);
        }
    }

    public static void writePathAndNode(final DataOutput out, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> node) throws IOException {
        try (NormalizedNodeDataOutput stream = NormalizedNodeInputOutput.newDataOutput(out)) {
            stream.writeYangInstanceIdentifier(path);
            stream.writeNormalizedNode(node);
        }
    }

    public static YangInstanceIdentifier deserializePath(final DataInput in) {
        try {
            NormalizedNodeDataInput streamReader = streamReader(in);
            return streamReader.readYangInstanceIdentifier();
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path", e);
        }
    }
}
