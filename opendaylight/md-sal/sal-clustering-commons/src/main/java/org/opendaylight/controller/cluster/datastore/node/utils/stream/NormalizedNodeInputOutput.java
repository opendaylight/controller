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
        return new VersionedNormalizedNodeDataInput(input).delegate();
    }

    /**
     * Creates a new {@link NormalizedNodeDataInput} instance that reads from the given input. This method does not
     * perform any initial validation of the input stream.
     *
     * @param input the DataInput to read from
     * @return a new {@link NormalizedNodeDataInput} instance
     */
    public static NormalizedNodeDataInput newDataInputWithoutValidation(final @NonNull DataInput input) {
        return new VersionedNormalizedNodeDataInput(input);
    }

    /**
     * Creates a new {@link NormalizedNodeDataOutput} instance that writes to the given output and latest current
     * stream version.
     *
     * @param output the DataOutput to write to
     * @return a new {@link NormalizedNodeDataOutput} instance
     */
    public static NormalizedNodeDataOutput newDataOutput(final @NonNull DataOutput output) {
        return new MagnesiumDataOutput(output);
    }

    /**
     * Creates a new {@link NormalizedNodeDataOutput} instance that writes to the given output.
     *
     * @param output the DataOutput to write to
     * @param version Streaming version to use
     * @return a new {@link NormalizedNodeDataOutput} instance
     */
    public static NormalizedNodeDataOutput newDataOutput(final @NonNull DataOutput output,
            final @NonNull NormalizedNodeStreamVersion version) {
        switch (version) {
            case LITHIUM:
                return new LithiumNormalizedNodeOutputStreamWriter(output);
            case NEON_SR2:
                return new NeonSR2NormalizedNodeOutputStreamWriter(output);
            case SODIUM_SR1:
                return new SodiumSR1DataOutput(output);
            case MAGNESIUM:
                return new MagnesiumDataOutput(output);
            default:
                throw new IllegalStateException("Unhandled version " + version);
        }
    }

}
