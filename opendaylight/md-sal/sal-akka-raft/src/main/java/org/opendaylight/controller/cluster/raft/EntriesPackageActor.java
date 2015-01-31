/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This actor receives messages from leader and perform serialization, if necessary.
 * Once package is prepared, it gets forwarded to heartbeat actor.
 * The purpose of this actor is to unblock the Leader raft actor.
 */

public class EntriesPackageActor extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(EntriesPackageActor.class);

    private final ActorRef heartbeatActor;

    public EntriesPackageActor(final ActorRef heartbeatActor) {
        this.heartbeatActor = heartbeatActor;
    }

    public static Props props(final ActorRef heartbeatActor) {
        return Props.create(new EntriesPackageCreator(heartbeatActor));
    }

    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof AppendEntries) {
            handleAppendEntries((AppendEntries) message);
        } else if (message instanceof InstallSnapshot) {
            heartbeatActor.tell(((InstallSnapshot) message).toSerializable(), getSelf());
        }
    }

    /*
     * If apendEntries contains a list of entry data, then send two messages,
     * one as heartbeat message data, other the real package.
     *
     */
    private void handleAppendEntries(AppendEntries appendEntries) {
        if (!appendEntries.getEntries().isEmpty()) {
            heartbeatActor.tell(appendEntries.toSerializable(), getSelf());
        }
        heartbeatActor.tell(appendEntries, getSelf());
    }


    @Override
    public void postStop() {
        try {
            super.postStop();
        } catch (Exception e) {
            LOG.warn("Failure while stopping entries package actor.");
        } finally {
            getContext().stop(heartbeatActor);
        }
    }

    private static class EntriesPackageCreator implements Creator<EntriesPackageActor> {

        private final ActorRef heartbeatActor;

        public EntriesPackageCreator(final ActorRef heartbeatActor) {
            this.heartbeatActor = heartbeatActor;
        }

        @Override
        public EntriesPackageActor create() throws Exception {
            return new EntriesPackageActor(heartbeatActor);
        }
    }
}
