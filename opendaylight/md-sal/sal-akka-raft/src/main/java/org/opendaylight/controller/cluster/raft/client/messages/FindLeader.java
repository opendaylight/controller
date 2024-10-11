/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;

/**
 * Request to locate the leader raft actor. Each {@link org.opendaylight.controller.cluster.raft.RaftActor} must
 * respond with a {@link FindLeaderReply} containing the address of the leader, as it is known to that particular
 * actor.
 *
 * <p>This message is intended for testing purposes only.
 */
@VisibleForTesting
public final class FindLeader implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final FindLeader INSTANCE = new FindLeader();

    private FindLeader() {
        // Hidden to force reuse
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
