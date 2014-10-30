/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

/**
 * Provides various utility methods for serialization and de-serialization.
 *
 * @author Thomas Pantelis
 */
public final class SerializationUtils {
    public static interface Applier<T> {
        void apply(T instance, YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    public static void serializePathAndNode(YangInstanceIdentifier path, NormalizedNode<?, ?> node,
            DataOutput output) {
        try {
            NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(output);
            NormalizedNodeWriter.forStreamWriter(streamWriter).write(node);
            streamWriter.writeYangInstanceIdentifier(path);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path {} and Node {}",
                    path, node), e);
        }
    }

    public static <T> void deserializePathAndNode(DataInput in, T instance, Applier<T> applier) {
        try {
            NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(in);
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            applier.apply(instance, path, node);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path and Node", e);
        }
    }
}
