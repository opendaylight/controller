/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

/**
 * Follower view of {@link RaftState}.
 */
public non-sealed interface FollowerRaftState extends RaftState.Mutable {
    /**
     * Appends an entry to the in-memory log and persists it as well.
     *
     * @param replicatedLogEntry the entry to append
     * @param callback the callback to be notified when persistence is complete (optional).
     * @param doAsync if true, the persistent actor can receive subsequent messages to process in between the persist
     *        call and the execution of the associated callback. If false, subsequent messages are stashed and get
     *        delivered after persistence is complete and the associated callback is executed. In either case the
     *        callback is guaranteed to execute in the context of the actor associated with this log.
     * @return true if the entry was successfully appended, false otherwise.
     */
    boolean appendAndPersist(@NonNull ReplicatedLogEntry replicatedLogEntry,
            @Nullable Consumer<ReplicatedLogEntry> callback, boolean doAsync);
}
