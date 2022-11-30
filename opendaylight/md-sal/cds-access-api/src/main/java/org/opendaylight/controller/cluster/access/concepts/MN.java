/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

/**
 * Serialization proxy for {@link MemberName}.
 */
final class MN implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private byte[] serialized;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public MN() {
        // for Externalizable
    }

    MN(final byte[] serialized) {
        this.serialized = requireNonNull(serialized);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(serialized.length);
        out.write(serialized);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        serialized = new byte[in.readInt()];
        in.readFully(serialized);
    }

    @java.io.Serial
    private Object readResolve() {
        // TODO: consider caching instances here
        return new MemberName(new String(serialized, StandardCharsets.UTF_8), serialized);
    }
}
