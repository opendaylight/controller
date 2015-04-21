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

    public LeaderStateChanged(@Nonnull String memberId, @Nullable String leaderId) {
        this.memberId = Preconditions.checkNotNull(memberId);
        this.leaderId = leaderId;
    }

    public @Nonnull String getMemberId() {
        return memberId;
    }

    public @Nullable String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LeaderStateChanged [memberId=").append(memberId).append(", leaderId=").append(leaderId)
                .append("]");
        return builder.toString();
    }
}
