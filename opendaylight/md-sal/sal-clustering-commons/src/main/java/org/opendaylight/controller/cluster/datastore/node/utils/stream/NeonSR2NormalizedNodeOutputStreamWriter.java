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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;

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
final class NeonSR2NormalizedNodeOutputStreamWriter extends AbstractLithiumDataOutput {
    private final Map<AugmentationIdentifier, Integer> aidCodeMap = new HashMap<>();
    private final Map<QNameModule, Integer> moduleCodeMap = new HashMap<>();
    private final Map<QName, Integer> qnameCodeMap = new HashMap<>();

    NeonSR2NormalizedNodeOutputStreamWriter(final DataOutput output) {
        super(output);
    }

    @Override
    protected short streamVersion() {
        return TokenTypes.NEON_SR2_VERSION;
    }

    @Override
    public void writeQName(final QName qname) throws IOException {
        final Integer value = qnameCodeMap.get(qname);
        if (value == null) {
            // Fresh QName, remember it and emit as three strings
            qnameCodeMap.put(qname, qnameCodeMap.size());
            writeByte(TokenTypes.IS_QNAME_VALUE);
            defaultWriteQName(qname);
        } else {
            // We have already seen this QName: write its code
            writeByte(TokenTypes.IS_QNAME_CODE);
            writeInt(value);
        }
    }

    @Override
    void writeAugmentationIdentifier(final AugmentationIdentifier aid) throws IOException {
        final Integer value = aidCodeMap.get(aid);
        if (value == null) {
            // Fresh AugmentationIdentifier, remember it and emit as three strings
            aidCodeMap.put(aid, aidCodeMap.size());
            writeByte(TokenTypes.IS_AUGMENT_VALUE);
            super.writeAugmentationIdentifier(aid);
        } else {
            // We have already seen this AugmentationIdentifier: write its code
            writeByte(TokenTypes.IS_AUGMENT_CODE);
            writeInt(value);
        }
    }

    @Override
    void writeModule(final QNameModule module) throws IOException {
        final Integer value = moduleCodeMap.get(module);
        if (value == null) {
            // Fresh QNameModule, remember it and emit as three strings
            moduleCodeMap.put(module, moduleCodeMap.size());
            writeByte(TokenTypes.IS_MODULE_VALUE);
            defaultWriteModule(module);
        } else {
            // We have already seen this QNameModule: write its code
            writeByte(TokenTypes.IS_MODULE_CODE);
            writeInt(value);
        }
    }
}
