/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link UpdateElectionTerm}.
 */
@Deprecated
final class UT implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private TermInfo termInfo;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public UT() {
        // For Externalizable
    }

    UT(final TermInfo termInfo) {
        this.termInfo = requireNonNull(termInfo);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLong(out, termInfo.term());
        out.writeObject(termInfo.votedFor());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        termInfo = new TermInfo(WritableObjects.readLong(in), (String) in.readObject());
    }

    @java.io.Serial
    private Object readResolve() {
        return new UpdateElectionTerm(termInfo);
    }
}
