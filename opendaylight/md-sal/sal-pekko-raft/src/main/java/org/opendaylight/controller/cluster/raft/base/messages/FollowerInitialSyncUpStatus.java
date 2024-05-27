/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The FollowerInitialSyncUpStatus is sent by a Follower to inform any RaftActor subclass whether the Follower
 * is at least at the same commitIndex as the Leader was when it sent the follower the very first heart beat.
 * This status can be used to determine if a Follower has caught up with the current Leader in an upgrade scenario
 * for example.
 */
public final class FollowerInitialSyncUpStatus {
    private final boolean initialSyncDone;
    private final String name;

    public FollowerInitialSyncUpStatus(final boolean initialSyncDone, final @NonNull String name) {
        this.initialSyncDone = initialSyncDone;
        this.name = requireNonNull(name);
    }

    public boolean isInitialSyncDone() {
        return initialSyncDone;
    }

    public @NonNull String getName() {
        return name;
    }
}
