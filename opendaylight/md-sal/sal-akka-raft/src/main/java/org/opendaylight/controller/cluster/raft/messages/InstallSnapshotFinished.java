/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class InstallSnapshotFinished extends AbstractRaftRPC {
    private static final long serialVersionUID = 1L;

    private final String followerId;
    private final boolean success;

    public InstallSnapshotFinished(final long term, final String followerId, final boolean success) {
        super(term);
        this.followerId = followerId;
        this.success = success;
    }

    public String getFollowerId() {
        return followerId;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "InstallSnapshotFinished [term=" + getTerm()
                + ", followerId=" + followerId
                + ", success=" + success + "]";
    }

    @Override
    Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private InstallSnapshotFinished installSnapshotFinished;

        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final InstallSnapshotFinished installSnapshotFinished) {
            this.installSnapshotFinished = installSnapshotFinished;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(installSnapshotFinished.getTerm());
            out.writeObject(installSnapshotFinished.followerId);
            out.writeBoolean(installSnapshotFinished.success);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String followerId = (String) in.readObject();
            boolean success = in.readBoolean();

            installSnapshotFinished = new InstallSnapshotFinished(term, followerId, success);
        }

        private Object readResolve() {
            return installSnapshotFinished;
        }
    }
}
