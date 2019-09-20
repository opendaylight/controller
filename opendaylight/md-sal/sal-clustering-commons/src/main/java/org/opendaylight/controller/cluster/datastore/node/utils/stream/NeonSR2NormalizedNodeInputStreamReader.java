/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Verify.verify;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

/**
 * Neon SR2 specialization of AbstractLithiumDataInput. Unlike its Lithium counterpart, this format uses coding for
 * QNameModules, QNames, NodeIdentifiers and AugmentationIdentifiers, thus reducing stream duplication.
 */
final class NeonSR2NormalizedNodeInputStreamReader extends AbstractLithiumDataInput {
    private final ArrayList<NodeIdentifier> codedNodeIdentifiers = new ArrayList<>();
    private final List<AugmentationIdentifier> codedAugments = new ArrayList<>();
    private final List<QNameModule> codedModules = new ArrayList<>();
    private final List<QName> codedQNames = new ArrayList<>();

    NeonSR2NormalizedNodeInputStreamReader(final DataInput input) {
        super(input);
    }

    @Override
    public NormalizedNodeStreamVersion getVersion() {
        return NormalizedNodeStreamVersion.NEON_SR2;
    }

    @Override
    public QName readQName() throws IOException {
        final byte valueType = readByte();
        switch (valueType) {
            case NeonSR2Tokens.IS_QNAME_CODE:
                return codedQName(readInt());
            case NeonSR2Tokens.IS_QNAME_VALUE:
                return rawQName();
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }

    @Override
    AugmentationIdentifier readAugmentationIdentifier() throws IOException {
        final byte valueType = readByte();
        switch (valueType) {
            case NeonSR2Tokens.IS_AUGMENT_CODE:
                return codedAugmentId(readInt());
            case NeonSR2Tokens.IS_AUGMENT_VALUE:
                return rawAugmentId();
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }

    @Override
    NodeIdentifier readNodeIdentifier() throws IOException {
        // NodeIdentifier rides on top of QName, with this method really saying 'interpret next QName as NodeIdentifier'
        // to do that we inter-mingle with readQName()
        final byte valueType = readByte();
        switch (valueType) {
            case NeonSR2Tokens.IS_QNAME_CODE:
                return codedNodeIdentifier(readInt());
            case NeonSR2Tokens.IS_QNAME_VALUE:
                return rawNodeIdentifier();
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }

    private QNameModule readModule() throws IOException {
        final byte valueType = readByte();
        switch (valueType) {
            case NeonSR2Tokens.IS_MODULE_CODE:
                return codedModule(readInt());
            case NeonSR2Tokens.IS_MODULE_VALUE:
                return rawModule();
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }

    private NodeIdentifier codedNodeIdentifier(final int code) throws IOException {
        final NodeIdentifier existing = codedNodeIdentifiers.size() > code ? codedNodeIdentifiers.get(code) : null;
        return existing != null ? existing : storeNodeIdentifier(code, codedQName(code));
    }

    private NodeIdentifier rawNodeIdentifier() throws IOException {
        // Capture size before it incremented
        final int code = codedQNames.size();
        return storeNodeIdentifier(code, rawQName());
    }

    private NodeIdentifier storeNodeIdentifier(final int code, final QName qname) {
        final NodeIdentifier ret = NodeIdentifier.create(qname);
        final int size = codedNodeIdentifiers.size();

        if (code >= size) {
            // Null-fill others
            codedNodeIdentifiers.ensureCapacity(code + 1);
            for (int i = size; i < code; ++i) {
                codedNodeIdentifiers.add(null);
            }

            codedNodeIdentifiers.add(ret);
        } else {
            final NodeIdentifier check = codedNodeIdentifiers.set(code, ret);
            verify(check == null);
        }

        return ret;
    }

    private QName codedQName(final int code) throws IOException {
        try {
            return codedQNames.get(code);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("QName code " + code + " was not found", e);
        }
    }

    private QName rawQName() throws IOException {
        final String localName = readCodedString();
        final QNameModule module = readModule();
        final QName qname = QNameFactory.create(module, localName);
        codedQNames.add(qname);
        return qname;
    }

    private AugmentationIdentifier codedAugmentId(final int code) throws IOException {
        try {
            return codedAugments.get(code);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("QName set code " + code + " was not found", e);
        }
    }

    private AugmentationIdentifier rawAugmentId() throws IOException {
        final AugmentationIdentifier aid = defaultReadAugmentationIdentifier();
        codedAugments.add(aid);
        return aid;
    }

    private QNameModule codedModule(final int code) throws IOException {
        try {
            return codedModules.get(code);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Module code " + code + " was not found", e);
        }
    }

    private QNameModule rawModule() throws IOException {
        final String namespace = readCodedString();
        final String revision = readCodedString();
        final QNameModule mod = QNameFactory.createModule(namespace, revision);
        codedModules.add(mod);
        return mod;
    }
}
