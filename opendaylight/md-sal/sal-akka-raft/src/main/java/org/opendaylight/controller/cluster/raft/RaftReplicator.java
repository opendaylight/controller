/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

/**
 * A RaftReplicator is responsible for replicating messages to any one follower.
 * Once it gets a message for replication it should keep trying to replicate it
 * to the remote follower indefinitely.
 * <p>
 * Any new messages that are sent to this actor while it is replicating a
 * message may need to be stashed till the current message has been successfully
 * replicated. When a message is successfully replicated the RaftReplicator
 * needs to inform the RaftActor of it.
 */
public class RaftReplicator extends UntypedActor {

    /**
     * The state of the follower as known to this replicator
     */
    private final FollowerLogInformation followerLogInformation;

    /**
     * The local RaftActor that created this replicator so that it could
     * replicate messages to the follower
     */
    private final ActorRef leader;


    /**
     * The remote RaftActor to whom the messages need to be replicated
     */
    private ActorSelection follower;

    public RaftReplicator(FollowerLogInformation followerLogInformation,
        ActorRef leader) {
        this.followerLogInformation = followerLogInformation;
        this.leader = leader;
    }



    @Override public void onReceive(Object message) throws Exception {

    }

    public static Props props(final FollowerLogInformation followerLogInformation,
        final ActorRef leader) {
        return Props.create(new Creator<RaftReplicator>() {

            @Override public RaftReplicator create() throws Exception {
                return new RaftReplicator(followerLogInformation, leader);
            }
        });
    }
}
