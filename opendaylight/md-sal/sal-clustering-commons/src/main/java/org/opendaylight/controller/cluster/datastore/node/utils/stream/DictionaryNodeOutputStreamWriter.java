/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.yangtools.yang.common.QName;

final class DictionaryNodeOutputStreamWriter extends AbstractNormalizedNodeDataOutput implements DictionaryNormalizedNodeDataOutput {

    DictionaryNodeOutputStreamWriter(final DataOutput output) throws IOException {
        this(output, null);
    }

    DictionaryNodeOutputStreamWriter(final DataOutput output, final NormalizedNodeOutputDictionary dictionary) throws IOException {
        super(output, dictionary == null ? new NormalizedNodeOutputDictionary() : dictionary);
        writeByte(dictionary == null ? TokenTypes.RESET_DICTIONARY : TokenTypes.KEEP_DICTIONARY);
    }

    @Override
    protected short streamVersion() {
        return TokenTypes.BERYLLIUM_VERSION;
    }

    @Override
    protected void writeQName(final QName qname) throws IOException {
        if (qname != null) {
            final Integer value = dictionary().lookupQName(qname);
            if (value == null) {
                dictionary().storeQName(qname);
                writeByte(TokenTypes.QNAME_DEFINITION);
                super.writeQName(qname);
            } else {
                writeByte(TokenTypes.QNAME_REFERENCE);
                writeInt(value);
            }
        } else {
            writeByte(TokenTypes.IS_NULL_VALUE);
        }
    }
}
