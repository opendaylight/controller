/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serialization proxy for {@link FollowerIdentifier}.
 */
final class FI implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String value;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public FI() {
        // For Externalizable
    }

    FI(final String value) {
        this.value = requireNonNull(value);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(value);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        value = (String) in.readObject();
    }

    @java.io.Serial
    private Object readResolve() {
        return new FollowerIdentifier(value);
    }
}
