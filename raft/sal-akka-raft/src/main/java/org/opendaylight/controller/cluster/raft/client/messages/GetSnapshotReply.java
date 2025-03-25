/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Reply to GetSnapshot that returns a serialized Snapshot instance.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public record GetSnapshotReply(String id, Snapshot snapshot) {
    public GetSnapshotReply {
        requireNonNull(id);
        requireNonNull(snapshot);
    }
}
