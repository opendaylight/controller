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
 * Serialization proxy for {@link ApplyJournalEntries}.
 */
final class AJE implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ApplyJournalEntries applyEntries;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public AJE() {
        // For Externalizable
    }

    AJE(final ApplyJournalEntries applyEntries) {
        this.applyEntries = requireNonNull(applyEntries);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLong(out, applyEntries.getToIndex());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        applyEntries = new ApplyJournalEntries(WritableObjects.readLong(in));
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(applyEntries);
    }
}
