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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Provides various utility methods for serialization and de-serialization.
 *
 * @author Thomas Pantelis
 */
public final class SerializationUtils {
    public static final ThreadLocal<NormalizedNodeDataOutput> REUSABLE_WRITER_TL = new ThreadLocal<>();
    public static final ThreadLocal<NormalizedNodeDataInput> REUSABLE_READER_TL = new ThreadLocal<>();

    public interface Applier<T> {
        void apply(T instance, YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    private static NormalizedNodeDataOutput streamWriter(DataOutput out) throws IOException {
        NormalizedNodeDataOutput streamWriter = REUSABLE_WRITER_TL.get();
        if (streamWriter == null) {
            streamWriter = NormalizedNodeInputOutput.newDataOutput(out);
        }

        return streamWriter;
    }

    private static NormalizedNodeDataInput streamReader(DataInput in) throws IOException {
        NormalizedNodeDataInput streamReader = REUSABLE_READER_TL.get();
        if (streamReader == null) {
            streamReader = NormalizedNodeInputOutput.newDataInput(in);
        }

        return streamReader;
    }

    public static void serializePathAndNode(YangInstanceIdentifier path, NormalizedNode<?, ?> node,
            DataOutput out) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(node);
        try {
            NormalizedNodeDataOutput streamWriter = streamWriter(out);
            streamWriter.writeNormalizedNode(node);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path %s and Node %s",
                    path, node), e);
        }
    }

    public static <T> void deserializePathAndNode(DataInput in, T instance, Applier<T> applier) {
        try {
            NormalizedNodeDataInput streamReader = streamReader(in);
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            applier.apply(instance, path, node);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path and Node", e);
        }
    }

    private static NormalizedNode<?, ?> tryDeserializeNormalizedNode(DataInput in) throws IOException {
        boolean present = in.readBoolean();
        if (present) {
            NormalizedNodeDataInput streamReader = streamReader(in);
            return streamReader.readNormalizedNode();
        }

        return null;
    }

    public static NormalizedNode<?, ?> deserializeNormalizedNode(DataInput in) {
        try {
            return tryDeserializeNormalizedNode(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
        }
    }

    public static NormalizedNode<?, ?> deserializeNormalizedNode(byte [] bytes) {
        try {
            return tryDeserializeNormalizedNode(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
        }
    }

    public static void serializeNormalizedNode(NormalizedNode<?, ?> node, DataOutput out) {
        try {
            out.writeBoolean(node != null);
            if (node != null) {
                NormalizedNodeDataOutput streamWriter = streamWriter(out);
                streamWriter.writeNormalizedNode(node);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing NormalizedNode %s",
                    node), e);
        }
    }

    public static byte [] serializeNormalizedNode(NormalizedNode<?, ?> node) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serializeNormalizedNode(node, new DataOutputStream(bos));
        return bos.toByteArray();
    }

    public static void serializePath(YangInstanceIdentifier path, DataOutput out) {
        Preconditions.checkNotNull(path);
        try {
            NormalizedNodeDataOutput streamWriter = streamWriter(out);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path %s", path), e);
        }
    }

    public static YangInstanceIdentifier deserializePath(DataInput in) {
        try {
            NormalizedNodeDataInput streamReader = streamReader(in);
            return streamReader.readYangInstanceIdentifier();
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path", e);
        }
    }
}
