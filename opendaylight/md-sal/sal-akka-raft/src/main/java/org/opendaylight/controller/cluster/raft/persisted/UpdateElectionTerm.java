/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * Message class to persist election term information.
 */
public class UpdateElectionTerm implements Serializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private UpdateElectionTerm updateElectionTerm;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final UpdateElectionTerm updateElectionTerm) {
            this.updateElectionTerm = updateElectionTerm;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(updateElectionTerm.currentTerm);
            out.writeObject(updateElectionTerm.votedFor);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            updateElectionTerm = new UpdateElectionTerm(in.readLong(), (String) in.readObject());
        }

        private Object readResolve() {
            return updateElectionTerm;
        }
    }

    private static final long serialVersionUID = 1L;

    private final long currentTerm;
    private final String votedFor;

    public UpdateElectionTerm(final long currentTerm, final String votedFor) {
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return "UpdateElectionTerm [currentTerm=" + currentTerm + ", votedFor=" + votedFor + "]";
    }
}

