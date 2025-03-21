/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * This message is sent by a Follower to inform any {@link RaftActor} subclass whether the Follower is at least at the
 * same commitIndex as the Leader was when it sent the follower the very first heart beat. This status can be used to
 * determine if a Follower has caught up with the current Leader in an upgrade scenario for example.
 *
 * @param memberId the member ID
 * @param initialSyncDone initial synchronization status, {@code true} if the follower has caught up
 */
@NonNullByDefault
public record FollowerInitialSyncUpStatus(String memberId, boolean initialSyncDone) {
    public FollowerInitialSyncUpStatus {
        requireNonNull(memberId);
    }
}
