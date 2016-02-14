/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract base class for a server configuration change reply.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractServerChangeReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String leaderHint;
    private final ServerChangeStatus status;

    AbstractServerChangeReply(final @Nonnull ServerChangeStatus status, final @Nullable String leaderHint) {
        this.status = Preconditions.checkNotNull(status);
        this.leaderHint = leaderHint;
    }

    @VisibleForTesting
    @Nonnull public final Optional<String> getLeaderHint() {
        return Optional.ofNullable(leaderHint);
    }

    @Nonnull public final ServerChangeStatus getStatus() {
        return status;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
            .add("status", status).add("leaderHint", leaderHint).toString();
    }
}
