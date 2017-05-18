/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * A local message initiated internally from the RaftActor when some state of a leader has changed.
 *
 * @author Thomas Pantelis
 */
public class LeaderStateChanged {
    private final String memberId;
    private final String leaderId;
    private final short leaderPayloadVersion;

    public LeaderStateChanged(@Nonnull String memberId, @Nullable String leaderId, short leaderPayloadVersion) {
        this.memberId = Preconditions.checkNotNull(memberId);
        this.leaderId = leaderId;
        this.leaderPayloadVersion = leaderPayloadVersion;
    }

    public @Nonnull String getMemberId() {
        return memberId;
    }

    public @Nullable String getLeaderId() {
        return leaderId;
    }

    public short getLeaderPayloadVersion() {
        return leaderPayloadVersion;
    }

    @Override
    public String toString() {
        return "LeaderStateChanged [memberId=" + memberId
                + ", leaderId=" + leaderId
                + ", leaderPayloadVersion=" + leaderPayloadVersion + "]";
    }
}
