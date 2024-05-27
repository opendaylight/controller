/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message sent from a leader to its followers to indicate leadership transfer is starting.
 *
 * @author Thomas Pantelis
 */
public final class LeaderTransitioning implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String leaderId;

    public LeaderTransitioning(final @NonNull String leaderId) {
        this.leaderId = requireNonNull(leaderId);
    }

    public @NonNull String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        return "LeaderTransitioning [leaderId=" + leaderId + "]";
    }
}
