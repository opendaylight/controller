/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Beta
public final class NormalizedNodeInputOutput {
    private NormalizedNodeInputOutput() {
        throw new UnsupportedOperationException();
    }

    public static NormalizedNodeDataInput newDataInput(@Nonnull final DataInput input) throws IOException {
        return newDictionaryDataInput(input, null);
    }

    public static DictionaryNormalizedNodeDataInput newDictionaryDataInput(@Nonnull final DataInput input,
            @Nullable final NormalizedNodeInputDictionary dictionary) throws IOException {
        final byte marker = input.readByte();
        if (marker != TokenTypes.SIGNATURE_MARKER) {
            throw new InvalidNormalizedNodeStreamException(String.format("Invalid signature marker: %d", marker));
        }

        final short version = input.readShort();
        switch (version) {
            case TokenTypes.BERYLLIUM_VERSION:
                return new DictionaryNodeInputStreamReader(input, dictionary);
            case TokenTypes.LITHIUM_VERSION:
                return new NormalizedNodeInputStreamReader(input, true);
            default:
                throw new InvalidNormalizedNodeStreamException(String.format("Unhandled stream version %s", version));
        }
    }

    public static NormalizedNodeDataOutput newDataOutput(@Nonnull final DataOutput output) throws IOException {
        return new NormalizedNodeOutputStreamWriter(output);
    }

    public static DictionaryNormalizedNodeDataOutput newDictionaryDataOutput(@Nonnull final DataOutput output,
            @Nullable final NormalizedNodeOutputDictionary dictionary) throws IOException {
        return new DictionaryNodeOutputStreamWriter(output, dictionary);
    }
}
