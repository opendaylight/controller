/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import akka.actor.ActorRef;
import java.io.Serializable;

/**
 * Message sent to leader to transfer leadership to a particular follower
 */
public class RequestLeadership implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String followerId;
    private final ActorRef sender;

    public RequestLeadership(final String followerId, final ActorRef sender) {
        this.followerId = followerId;
        this.sender = sender;
    }

    public String getFollowerId() {
        return followerId;
    }

    public ActorRef getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "RequestLeadership [followerId=" + followerId + ", sender=" + sender + "]";
    }
}
