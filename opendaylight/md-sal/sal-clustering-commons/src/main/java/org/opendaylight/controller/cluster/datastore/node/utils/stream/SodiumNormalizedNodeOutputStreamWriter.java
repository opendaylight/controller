/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import java.util.Set;
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
 * <p>Based on the each node, the node type is also written to the stream, that helps in reconstructing the object,
 * while reading.
 */
class SodiumNormalizedNodeOutputStreamWriter extends LithiumNormalizedNodeOutputStreamWriter {
    private final Map<QName, Integer> qnameCodeMap = new HashMap<>();
    private final Map<Set<QName>, Integer> qnameSetCodeMap = new HashMap<>();

    SodiumNormalizedNodeOutputStreamWriter(final DataOutput output) {
        super(output);
    }

    @Override
    protected short streamVersion() {
        return TokenTypes.SODIUM_VERSION;
    }

    @Override
    protected final void writeQName(final QName qname) throws IOException {
        final Integer value = qnameCodeMap.get(qname);
        if (value == null) {
            // Fresh QName, remember it and emit as three strings
            qnameCodeMap.put(qname, qnameCodeMap.size());
            writeByte(TokenTypes.IS_QNAME_VALUE);
            super.writeQName(qname);
        } else {
            // We have already seen this QName: write its code
            writeByte(TokenTypes.IS_QNAME_CODE);
            writeInt(value);
        }
    }

    @Override
    void writeQNameSet(final Set<QName> qnames) throws IOException {
        final Integer value = qnameSetCodeMap.get(qnames);
        if (value == null) {
            // Fresh QName, remember it and emit as three strings
            qnameSetCodeMap.put(qnames, qnameSetCodeMap.size());
            writeByte(TokenTypes.IS_QNAMESET_VALUE);
            super.writeQNameSet(qnames);
        } else {
            // We have already seen this QName set: write its code
            writeByte(TokenTypes.IS_QNAMESET_CODE);
            writeInt(value);
        }
    }
}
