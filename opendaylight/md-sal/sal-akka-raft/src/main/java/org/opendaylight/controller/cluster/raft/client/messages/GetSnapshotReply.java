/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Reply to GetSnapshot that returns a serialized Snapshot instance.
 *
 * @author Thomas Pantelis
 */
public class GetSnapshotReply {
    private final String id;
    private final Snapshot snapshot;

    public GetSnapshotReply(@NonNull String id, @NonNull Snapshot snapshot) {
        this.id = requireNonNull(id);
        this.snapshot = requireNonNull(snapshot);
    }

    public @NonNull String getId() {
        return id;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposes a mutable object stored in a field but "
            + "this is OK since this class is merely a DTO and does not process the byte[] internally. "
            + "Also it would be inefficient to create a return copy as the byte[] could be large.")
    public @NonNull Snapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public String toString() {
        return "GetSnapshotReply [id=" + id + ", snapshot=" + snapshot + "]";
    }
}
