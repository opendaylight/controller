/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Registration;

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
        return new NormalizedNodeOutputStreamWriter(output);
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
            case SODIUM:
                return new SodiumNormalizedNodeOutputStreamWriter(output);
            default:
                throw new IllegalStateException("Unhandled version " + version);
        }
    }

    private static final ThreadLocal<Entry<DataInput, NormalizedNodeDataInput>> TL_INPUT =
            new ThreadLocal<>();
    private static final ThreadLocal<Entry<DataOutput, NormalizedNodeDataOutput>> TL_OUTPUT =
            new ThreadLocal<>();

    public static Registration setupLazyDataInput(final @NonNull DataInput input) {
        verify(TL_INPUT.get() == null);

        final Entry<DataInput, NormalizedNodeDataInput> state = new SimpleImmutableEntry<>(input,
                newDataInputWithoutValidation(input));
        TL_INPUT.set(state);
        return () -> {
            final Entry<DataInput, NormalizedNodeDataInput> local = verifyNotNull(TL_INPUT.get(),
                "No thread-local state present");
            checkState(local.equals(state), "Unexpected thread-local state %s when cleaning %s", local, state);
            TL_INPUT.set(null);
        };
    }

    public static NormalizedNodeDataInput coerceDataInput(final @NonNull DataInput input) {
        final Entry<DataInput, NormalizedNodeDataInput> local = verifyNotNull(TL_INPUT.get(),
            "No thread-local output set up");
        final NormalizedNodeDataInput stream = local.getValue();
        checkState(local.getKey().equals(input), "Mismatches thread-local output %s and requested input %s",
            stream, input);
        return stream;
    }
}
