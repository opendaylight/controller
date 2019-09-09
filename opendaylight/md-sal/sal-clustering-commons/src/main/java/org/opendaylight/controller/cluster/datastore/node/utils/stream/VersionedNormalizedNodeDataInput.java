/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.IOException;

final class VersionedNormalizedNodeDataInput extends ForwardingNormalizedNodeDataInput {
    private DataInput input;
    private NormalizedNodeDataInput delegate;

    VersionedNormalizedNodeDataInput(final DataInput input) {
        this.input = requireNonNull(input);
    }

    @Override
    NormalizedNodeDataInput delegate() throws IOException {
        if (delegate != null) {
            return delegate;
        }

        final byte marker = input.readByte();
        if (marker != TokenTypes.SIGNATURE_MARKER) {
            throw defunct("Invalid signature marker: %d", marker);
        }

        final short version = input.readShort();
        final NormalizedNodeDataInput ret;
        switch (version) {
            case TokenTypes.LITHIUM_VERSION:
                ret = new LithiumNormalizedNodeInputStreamReader(input);
                break;
            case TokenTypes.NEON_SR2_VERSION:
                ret = new NeonSR2NormalizedNodeInputStreamReader(input);
                break;
            case TokenTypes.SODIUM_SR1_VERSION:
                ret = new SodiumSR1DataInput(input);
                break;
            default:
                throw defunct("Unhandled stream version %s", version);
        }

        setDelegate(ret);
        return ret;
    }

    private InvalidNormalizedNodeStreamException defunct(final String format, final Object... args) {
        final InvalidNormalizedNodeStreamException ret = new InvalidNormalizedNodeStreamException(
            String.format(format, args));
        // Make sure the stream is not touched
        setDelegate(new ForwardingNormalizedNodeDataInput() {
            @Override
            NormalizedNodeDataInput delegate() throws IOException {
                throw new InvalidNormalizedNodeStreamException("Stream is not usable", ret);
            }
        });
        return ret;
    }

    private void setDelegate(final NormalizedNodeDataInput delegate) {
        this.delegate = requireNonNull(delegate);
        input = null;
    }
}
