/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;

final class SodiumNormalizedNodeInputStreamReader extends LithiumNormalizedNodeInputStreamReader {
    private final List<AugmentationIdentifier> codedAugments = new ArrayList<>();
    private final List<QName> codedQNames = new ArrayList<>();

    SodiumNormalizedNodeInputStreamReader(final DataInput input) {
        super(input);
    }

    @Override
    QName readQName() throws IOException {
        final byte valueType = readByte();
        switch (valueType) {
            case TokenTypes.IS_QNAME_CODE:
                return codedQName(readInt());
            case TokenTypes.IS_QNAME_VALUE:
                return rawQName();
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }

    @Override
    AugmentationIdentifier readAugmentationIdentifier() throws IOException {
        final byte valueType = readByte();
        switch (valueType) {
            case TokenTypes.IS_AUGMENT_CODE:
                return codecAugmentId(readInt());
            case TokenTypes.IS_AUGMENT_VALUE:
                return rawAugmentId();
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }

    private QName codedQName(final int code) throws IOException {
        try {
            return codedQNames.get(code);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("QName code " + code + " was not found", e);
        }
    }

    private QName rawQName() throws IOException {
        final QName qname = super.readQName();
        codedQNames.add(qname);
        return qname;
    }

    private AugmentationIdentifier codecAugmentId(final int code) throws IOException {
        try {
            return codedAugments.get(code);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("QName set code " + code + " was not found", e);
        }
    }

    private AugmentationIdentifier rawAugmentId() throws IOException {
        final AugmentationIdentifier aid = super.readAugmentationIdentifier();
        codedAugments.add(aid);
        return aid;
    }
}
