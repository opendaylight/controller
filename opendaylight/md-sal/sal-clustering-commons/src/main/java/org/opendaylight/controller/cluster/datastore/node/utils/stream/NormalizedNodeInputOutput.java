/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion.LITHIUM;
import static org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion.MAGNESIUM;
import static org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion.NEON_SR2;
import static org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion.SODIUM_SR1;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;

@Beta
@Deprecated(forRemoval = true)
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
        return new CompatNormalizedNodeDataInput(org.opendaylight.yangtools.yang.data.codec.binfmt
            .NormalizedNodeDataInput.newDataInput(input));
    }

    /**
     * Creates a new {@link NormalizedNodeDataInput} instance that reads from the given input. This method does not
     * perform any initial validation of the input stream.
     *
     * @param input the DataInput to read from
     * @return a new {@link NormalizedNodeDataInput} instance
     */
    public static NormalizedNodeDataInput newDataInputWithoutValidation(final @NonNull DataInput input) {
        return new CompatNormalizedNodeDataInput(org.opendaylight.yangtools.yang.data.codec.binfmt
            .NormalizedNodeDataInput.newDataInputWithoutValidation(input));
    }

    /**
     * Creates a new {@link NormalizedNodeDataOutput} instance that writes to the given output and latest current
     * stream version.
     *
     * @param output the DataOutput to write to
     * @return a new {@link NormalizedNodeDataOutput} instance
     */
    public static NormalizedNodeDataOutput newDataOutput(final @NonNull DataOutput output) {
        return newDataOutput(output, NormalizedNodeStreamVersion.MAGNESIUM);
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
        final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput delegate;
        switch (version) {
            case LITHIUM:
                delegate = LITHIUM.newDataOutput(output);
                break;
            case NEON_SR2:
                delegate = NEON_SR2.newDataOutput(output);
                break;
            case SODIUM_SR1:
                delegate = SODIUM_SR1.newDataOutput(output);
                break;
            case MAGNESIUM:
                delegate = MAGNESIUM.newDataOutput(output);
                break;
            default:
                throw new IllegalStateException("Unhandled version " + version);
        }
        return new CompatNormalizedNodeDataOutput(delegate);
    }
}
