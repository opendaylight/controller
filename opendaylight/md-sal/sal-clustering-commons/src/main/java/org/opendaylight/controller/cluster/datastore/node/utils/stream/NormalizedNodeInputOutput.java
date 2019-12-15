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
        try {
            return new CompatNormalizedNodeDataInput(org.opendaylight.yangtools.yang.data.codec.binfmt
                .NormalizedNodeDataInput.newDataInput(input));
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
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
        return new CompatNormalizedNodeDataOutput(version.toYangtools().newDataOutput(output));
    }
}
