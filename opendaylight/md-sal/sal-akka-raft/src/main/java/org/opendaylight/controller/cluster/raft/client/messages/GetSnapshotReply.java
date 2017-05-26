/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Reply to GetSnapshot that returns a serialized Snapshot instance.
 *
 * @author Thomas Pantelis
 */
public class GetSnapshotReply {
    private final String id;
    private final Snapshot snapshot;

    public GetSnapshotReply(@Nonnull String id, @Nonnull Snapshot snapshot) {
        this.id = Preconditions.checkNotNull(id);
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposes a mutable object stored in a field but "
            + "this is OK since this class is merely a DTO and does not process the byte[] internally. "
            + "Also it would be inefficient to create a return copy as the byte[] could be large.")
    @Nonnull
    public Snapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public String toString() {
        return "GetSnapshotReply [id=" + id + ", snapshot=" + snapshot + "]";
    }
}
