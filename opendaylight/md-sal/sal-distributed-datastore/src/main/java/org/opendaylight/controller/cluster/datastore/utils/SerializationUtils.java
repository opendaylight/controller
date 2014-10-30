/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import com.google.protobuf.ByteString;

/**
 * Provides various utility methods for serialization and de-serialization.
 *
 * @author Thomas Pantelis
 */
public final class SerializationUtils {
    public static interface Creator<T> {
        T newInstance(YangInstanceIdentifier path, NormalizedNode<?, ?> node);
    }

    public static ByteString pathAndNodeToByteString(YangInstanceIdentifier path,
            NormalizedNode<?, ?> node) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            NormalizedNodeOutputStreamWriter streamWriter = new NormalizedNodeOutputStreamWriter(bos);
            NormalizedNodeWriter.forStreamWriter(streamWriter).write(node);
            streamWriter.writeYangInstanceIdentifier(path);
            return ByteString.copyFrom(bos.toByteArray());
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Error serializing path {} and Node {}",
                    path, node), e);
        }
    }

    public static <T> T pathAndNodeFromByteString(ByteString bytes, Creator<T> creator) {
        try {
            NormalizedNodeInputStreamReader streamReader = new NormalizedNodeInputStreamReader(
                    new ByteArrayInputStream(bytes.toByteArray()));
            NormalizedNode<?, ?> node = streamReader.readNormalizedNode();
            YangInstanceIdentifier path = streamReader.readYangInstanceIdentifier();
            return creator.newInstance(path, node);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error deserializing path and Node", e);
        }
    }
}
