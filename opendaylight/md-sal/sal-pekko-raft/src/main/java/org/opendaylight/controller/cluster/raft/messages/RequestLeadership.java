/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.apache.pekko.actor.ActorRef;

/**
 * Message sent to leader to transfer leadership to a particular follower.
 */
public final class RequestLeadership implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestedFollowerId;
    private final ActorRef replyTo;

    public RequestLeadership(final String requestedFollowerId, final ActorRef replyTo) {
        this.requestedFollowerId = requireNonNull(requestedFollowerId);
        this.replyTo = requireNonNull(replyTo);
    }

    public String getRequestedFollowerId() {
        return requestedFollowerId;
    }

    public ActorRef getReplyTo() {
        return replyTo;
    }

    @Override
    public String toString() {
        return "RequestLeadership [requestedFollowerId=" + requestedFollowerId + ", replyTo=" + replyTo + "]";
    }
}
