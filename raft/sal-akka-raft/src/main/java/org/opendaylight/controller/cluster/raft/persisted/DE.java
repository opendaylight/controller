/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link DeleteEntries}.
 */
final class DE implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private DeleteEntries deleteEntries;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public DE() {
        // For Externalizable
    }

    DE(final DeleteEntries deleteEntries) {
        this.deleteEntries = requireNonNull(deleteEntries);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLong(out, deleteEntries.getFromIndex());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        deleteEntries = new DeleteEntries(WritableObjects.readLong(in));
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(deleteEntries);
    }
}
