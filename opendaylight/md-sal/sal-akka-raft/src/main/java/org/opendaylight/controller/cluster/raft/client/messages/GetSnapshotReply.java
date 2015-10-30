/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;

/**
 * Reply to GetSnapshot that returns a serialized Snapshot instance if persistence is enabled.
 *
 * @author Thomas Pantelis
 */
public class GetSnapshotReply {
    private final Optional<byte[]> snapshot;

    public GetSnapshotReply(byte[] snapshot) {
        this.snapshot = Optional.fromNullable(snapshot);
    }

    /**
     * @return an Optional serialized Snapshot whose value if present if persistence is enabled.
     */
    @Nonnull
    public Optional<byte[]> getSnapshot() {
        return snapshot;
    }

    @Override
    public String toString() {
        return "GetSnapshotReply [snapshot present=" + snapshot.isPresent() + "]";
    }
}
