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
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.raft.internal.messages.SendHeartBeat;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

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
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor
     *
     * Since this is set to 100 milliseconds the Election timeout should be
     * at least 200 milliseconds
     *
     */
    private static final FiniteDuration HEART_BEAT_INTERVAL =
        new FiniteDuration(100, TimeUnit.MILLISECONDS);

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

    private Cancellable heartbeatCancel = null;

    public RaftReplicator(FollowerLogInformation followerLogInformation,
        ActorRef leader) {

        this.followerLogInformation = followerLogInformation;
        this.leader = leader;
        this.follower = getContext().actorSelection(followerLogInformation.getId());

        // Immediately schedule a heartbeat
        // Upon election: send initial empty AppendEntries RPCs
        // (heartbeat) to each server; repeat during idle periods to
        // prevent election timeouts (§5.2)
        scheduleHeartBeat(new FiniteDuration(0, TimeUnit.SECONDS));
    }

    private void scheduleHeartBeat(FiniteDuration interval) {
        if(heartbeatCancel != null && ! heartbeatCancel.isCancelled()){
            heartbeatCancel.cancel();
        }

        // Schedule a heartbeat. When the scheduler triggers the replicator
        // will let the RaftActor (leader) know that a new heartbeat needs to be sent
        // Scheduling the heartbeat only once here because heartbeats do not
        // need to be sent if there are other messages being sent to the remote
        // actor.
        heartbeatCancel =
            getContext().system().scheduler().scheduleOnce(interval,
                leader, new SendHeartBeat(), getContext().dispatcher(), getSelf());
    }



    @Override public void onReceive(Object message) throws Exception {
        scheduleHeartBeat(HEART_BEAT_INTERVAL);
        follower.forward(message, getContext());
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
