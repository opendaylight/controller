/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import akka.japi.Creator;
import akka.util.Timeout;
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * This actor belongs to the leader. Once leader is elected, a heartbeat actor gets created
 * per follower.
 *
 * Heartbeat actor will keep sending heartbeat message to the followers at selected time interval.
 * These heartbeat messages will make sure follower is always in contact. Whenever a new message with data comes from
 * leader, they will be forwarded to follower and reply of that message from follower will be redirected to the
 * leader. Thus, this actor works as a single point of message delivery to the follower.
 *
 */

public class HeartbeatActor extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatActor.class);
    private final ActorSelection followerActor;
    private final ActorRef leader;
    private final FiniteDuration interval;
    private static final Timeout TIMEOUT = new Timeout(100, TimeUnit.MILLISECONDS);
    private Cancellable heartbeatSchedule = null;
    private Object heartbeatPayload;

    public HeartbeatActor(final ActorSelection followerActor, final ActorRef leader, final FiniteDuration interval) {
        this.followerActor = followerActor;
        this.leader = leader;
        this.interval = interval;
    }

    public static Props props(final ActorSelection followerActor, final ActorRef leader, final FiniteDuration interval) {
        return Props.create(new HeartbeatCreator(followerActor, leader, interval));
    }


    @Override
    protected void handleReceive(Object message) throws Exception {

        if(message instanceof AppendEntriesMessages.AppendEntries) {
            // It should be AppendEntries message in Serialized format
            followerActor.tell(message, leader);
        } else if(message instanceof AppendEntries) {
            // Message to update heartbeat payload
            handleHeartbeatPayload((AppendEntries) message);

        } else if(message instanceof InstallSnapshotMessages.InstallSnapshot){
            followerActor.tell(message, leader);
        }

    }

    @Override
    public void postStop() {
        try {
            super.postStop();
        } catch (Exception e) {
            LOG.warn("Failure while stopping heartbeat actor.");
        } finally {
            if(heartbeatSchedule != null) {
                heartbeatSchedule.cancel();
            }
        }
    }

    private void handleHeartbeatPayload(AppendEntries message) {
        boolean setScheduler = heartbeatPayload == null? true:false;

        if(!message.getEntries().isEmpty()) {
            // no need to send list data in heartbeat message
            heartbeatPayload = new AppendEntries(message.getTerm(), message.getLeaderId(), message.getPrevLogIndex(),
                message.getPrevLogTerm(), Collections.<ReplicatedLogEntry>emptyList(), message.getLeaderCommit()).toSerializable();
        } else {
            heartbeatPayload = message.toSerializable();
        }
        Optional<ActorRef> followerActorRef =  getSingleActorRefFromSelector(followerActor);

        if(setScheduler && followerActorRef.isPresent()) {
            LOG.info("Starting the heartbeat scheduler for leader {} ", leader.path());
            heartbeatSchedule = getContext().system().scheduler().schedule(Duration.Zero(), interval,
                followerActorRef.get(), heartbeatPayload, getContext().system().dispatcher(), leader);
        }

    }


    private Optional<ActorRef> getSingleActorRefFromSelector(ActorSelection sel) {
        try {
            Future<ActorRef> fut = sel.resolveOne(TIMEOUT);
            ActorRef ref = Await.result(fut, TIMEOUT.duration());
            return Optional.of(ref);
        } catch (Exception e) { // Await throws java.lang.exception
            LOG.warn("No follower actor found for actorselection ", sel);
            return Optional.absent();
        }
    }

    private static class HeartbeatCreator implements Creator<HeartbeatActor> {
        private static final long serialVersionUID = 1L;

        private final ActorSelection followerActor;
        private final ActorRef leader;
        private final FiniteDuration interval;

        private HeartbeatCreator(final ActorSelection followerActor, final ActorRef leader, final FiniteDuration interval) {
            this.followerActor = followerActor;
            this.leader = leader;
            this.interval = interval;
        }


        @Override
        public HeartbeatActor create() throws Exception {
            return new HeartbeatActor(followerActor, leader, interval);
        }
    }
}
