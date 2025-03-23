/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Abstract base class for a server configuration change reply.
 *
 * @author Thomas Pantelis
 */
public abstract sealed class AbstractServerChangeReply implements Serializable
        permits AddServerReply, RemoveServerReply, ServerChangeReply {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String leaderHint;
    private final ServerChangeStatus status;

    AbstractServerChangeReply(final @NonNull ServerChangeStatus status, final @Nullable String leaderHint) {
        this.status = requireNonNull(status);
        this.leaderHint = leaderHint;
    }

    @VisibleForTesting
    public final @NonNull Optional<String> getLeaderHint() {
        return Optional.ofNullable(leaderHint);
    }

    public final @NonNull ServerChangeStatus getStatus() {
        return status;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
            .add("status", status).add("leaderHint", leaderHint).toString();
    }
}
