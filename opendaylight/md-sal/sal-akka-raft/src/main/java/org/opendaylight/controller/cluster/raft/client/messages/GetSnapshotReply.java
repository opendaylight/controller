/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Reply to GetSnapshot that returns a serialized Snapshot instance.
 *
 * @author Thomas Pantelis
 */
public class GetSnapshotReply {
    private final String id;
    private final byte[] snapshot;

    public GetSnapshotReply(@Nonnull String id, @Nonnull byte[] snapshot) {
        this.id = Preconditions.checkNotNull(id);
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public byte[] getSnapshot() {
        return snapshot;
    }

    @Override
    public String toString() {
        return "GetSnapshotReply [id=" + id + ", snapshot.length=" + snapshot.length + "]";
    }
}
