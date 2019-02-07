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
import org.eclipse.jdt.annotation.NonNull;

@Beta
public final class NormalizedNodeInputOutput {
    private NormalizedNodeInputOutput() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new {@link NormalizedNodeDataInput} instance that reads from the given input. This method first reads
     * and validates that the input contains a valid NormalizedNode stream.
     *
     * @param input the DataInput to read from
     * @return a new {@link NormalizedNodeDataInput} instance
     * @throws IOException if an error occurs reading from the input
     */
    public static NormalizedNodeDataInput newDataInput(final @NonNull DataInput input) throws IOException {
        final byte marker = input.readByte();
        if (marker != TokenTypes.SIGNATURE_MARKER) {
            throw new InvalidNormalizedNodeStreamException(String.format("Invalid signature marker: %d", marker));
        }

        final short version = input.readShort();
        switch (version) {
            case TokenTypes.LITHIUM_VERSION:
                return new NormalizedNodeInputStreamReader(input, true);
            default:
                throw new InvalidNormalizedNodeStreamException(String.format("Unhandled stream version %s", version));
        }
    }

    /**
     * Creates a new {@link NormalizedNodeDataInput} instance that reads from the given input. This method does not
     * perform any initial validation of the input stream.
     *
     * @param input the DataInput to read from
     * @return a new {@link NormalizedNodeDataInput} instance
     */
    public static NormalizedNodeDataInput newDataInputWithoutValidation(final @NonNull DataInput input) {
        return new NormalizedNodeInputStreamReader(input, false);
    }

    /**
     * Creates a new {@link NormalizedNodeDataOutput} instance that writes to the given output.
     *
     * @param output the DataOutput to write to
     * @return a new {@link NormalizedNodeDataOutput} instance
     */
    public static NormalizedNodeDataOutput newDataOutput(final @NonNull DataOutput output) {
        return new NormalizedNodeOutputStreamWriter(output);
    }
}
