/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Provides various utility methods for serialization and de-serialization.
 *
 * @author Thomas Pantelis
 */
public final class SerializationUtils {
    public static ThreadLocal<NormalizedNodeOutputStreamWriter> REUSABLE_WRITER_TL = new ThreadLocal<>();
    public static ThreadLocal<NormalizedNodeInputStreamReader> REUSABLE_READER_TL = new ThreadLocal<>();

    public static interface Applier<T> {
        void apply(T instance, YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    private static NormalizedNodeOutputStreamWriter streamWriter(DataOutput out) throws IOException {
        NormalizedNodeOutputStreamWriter streamWriter = REUSABLE_WRITER_TL.get();
        if(streamWriter == null) {
            streamWriter = new NormalizedNodeOutputStreamWriter(out);
        }

        return streamWriter;
    }

    private static NormalizedNodeInputStreamReader streamReader(DataInput in) throws IOException {
        NormalizedNodeInputStreamReader streamWriter = REUSABLE_READER_TL.get();
        if(streamWriter == null) {
            streamWriter = new NormalizedNodeInputStreamReader(in);
        }

        return streamWriter;
    }

    public static void serializePathAndNode(YangInstanceIdentifier path, NormalizedNode<?, ?> node,
            DataOutput out) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(node);
        try {
            NormalizedNodeOutputStreamWriter streamWriter = streamWriter(out);
            streamWriter.writeNormalizedNode(node);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path %s and Node %s",
                    path, node), e);
        }
    }

    public static <T> void deserializePathAndNode(DataInput in, T instance, Applier<T> applier) {
        try {
            NormalizedNodeInputStreamReader streamReader = streamReader(in);
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            applier.apply(instance, path, node);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path and Node", e);
        }
    }

    public static void serializeNormalizedNode(NormalizedNode<?, ?> node, DataOutput out) {
        try {
            out.writeBoolean(node != null);
            if(node != null) {
                NormalizedNodeOutputStreamWriter streamWriter = streamWriter(out);
                streamWriter.writeNormalizedNode(node);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing NormalizedNode %s",
                    node), e);
        }
    }

    public static NormalizedNode<?, ?> deserializeNormalizedNode(DataInput in) {
            try {
                boolean present = in.readBoolean();
                if(present) {
                    NormalizedNodeInputStreamReader streamReader = streamReader(in);
                    return streamReader.readNormalizedNode();
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Error deserializing NormalizedNode", e);
            }

        return null;
    }

    public static void serializePath(YangInstanceIdentifier path, DataOutput out) {
        Preconditions.checkNotNull(path);
        try {
            NormalizedNodeOutputStreamWriter streamWriter = streamWriter(out);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path %s", path), e);
        }
    }

    public static YangInstanceIdentifier deserializePath(DataInput in) {
        try {
            NormalizedNodeInputStreamReader streamReader = streamReader(in);
            return streamReader.readYangInstanceIdentifier();
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path", e);
        }
    }
}
