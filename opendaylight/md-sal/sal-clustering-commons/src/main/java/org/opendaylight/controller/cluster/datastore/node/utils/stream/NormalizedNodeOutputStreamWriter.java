/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * NormalizedNodeOutputStreamWriter will be used by distributed datastore to send normalized node in
 * a stream.
 * A stream writer wrapper around this class will write node objects to stream in recursive manner.
 * for example - If you have a ContainerNode which has a two LeafNode as children, then
 * you will first call
 * {@link #startContainerNode(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier, int)},
 * then will call
 * {@link #leafNode(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier, Object)} twice
 * and then, {@link #endNode()} to end container node.
 *
 * Based on the each node, the node type is also written to the stream, that helps in reconstructing the object,
 * while reading.
 */
final class NormalizedNodeOutputStreamWriter extends AbstractNormalizedNodeDataOutput {
    private final Map<String, Integer> stringCodeMap = new HashMap<>();

    NormalizedNodeOutputStreamWriter(final DataOutput output) {
        super(output);
    }

    @Override
    protected final short streamVersion() {
        return TokenTypes.LITHIUM_VERSION;
    }

    @Override
    protected void writeQName(final QName qname) throws IOException {
        writeString(qname.getLocalName());
        writeString(qname.getNamespace().toString());
        writeString(qname.getFormattedRevision());
    }

    @Override
    protected void writeString(final String string) throws IOException {
        if (string != null) {
            final Integer value = stringCodeMap.get(string);
            if (value == null) {
                stringCodeMap.put(string, stringCodeMap.size());
                writeByte(TokenTypes.IS_STRING_VALUE);
                writeUTF(string);
            } else {
                writeByte(TokenTypes.IS_CODE_VALUE);
                writeInt(value);
            }
        } else {
            writeByte(TokenTypes.IS_NULL_VALUE);
        }
    }
}
