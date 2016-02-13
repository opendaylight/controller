/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * The FollowerInitialSyncUpStatus is sent by a Follower to inform any RaftActor subclass whether the Follower
 * is at least at the same commitIndex as the Leader was when it sent the follower the very first heartbeat.
 *
 * This status can be used to determine if a Follower has caught up with the current Leader in an upgrade scenario
 * for example.
 */
public final class FollowerInitialSyncUpStatus {
    private final boolean initialSyncDone;
    private final String name;

    public FollowerInitialSyncUpStatus(final boolean initialSyncDone, @Nonnull final String name) {
        this.initialSyncDone = initialSyncDone;
        this.name = Preconditions.checkNotNull(name);
    }

    public boolean isInitialSyncDone() {
        return initialSyncDone;
    }

    @Nonnull public String getName() {
        return name;
    }
}
