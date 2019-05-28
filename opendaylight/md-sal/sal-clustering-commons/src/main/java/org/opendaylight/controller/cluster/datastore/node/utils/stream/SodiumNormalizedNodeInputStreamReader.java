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

final class SodiumNormalizedNodeInputStreamReader extends LithiumNormalizedNodeInputStreamReader {
    private final List<QName> codedQNames = new ArrayList<>();

    SodiumNormalizedNodeInputStreamReader(final DataInput input) {
        super(input);
    }

    @Override
    QName readQName() throws IOException {
        final byte valueType = readByte();
        switch (valueType) {
            case TokenTypes.IS_QNAME_CODE:
                final int code = readInt();
                try {
                    return codedQNames.get(code);
                } catch (IndexOutOfBoundsException e) {
                    throw new IOException("QName code " + code + " was not found", e);
                }
            case TokenTypes.IS_QNAME_VALUE:
                // Read in the same sequence of writing
                final QName qname = super.readQName();
                codedQNames.add(qname);
                return qname;
            default:
                throw new IOException("Unhandled QName value type " + valueType);
        }
    }
}
