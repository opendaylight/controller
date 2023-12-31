/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.dispatch.ControlMessage;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An {@link RaftActor} supervision event. These events are reported to the supervising actor.
 */
@NonNullByDefault
public sealed interface RaftActorEvent extends ControlMessage {
    /**
     * Returns the reference to {@link RaftActor} reporting the event.
     *
     * @return the reference to {@link RaftActor} reporting the event
     */
    ActorRef raftActor();

    /**
     * Reported every time {@link RaftActor} completes recovery and enters operational state.
     */
    public record Recovered(ActorRef raftActor) implements RaftActorEvent {
        public Recovered {
            requireNonNull(raftActor);
        }
    }

    /**
     * Reported when a behavior/role changes.
     */
    public record RoleChanged(ActorRef raftActor, String memberId, @Nullable RaftState oldRole, RaftState newRole)
            implements RaftActorEvent {
        public RoleChanged {
            requireNonNull(raftActor);
        }
    }

    /**
     * Reported when knowledge about the leader changes.
     */
    public non-sealed class LeaderChanged implements RaftActorEvent {
        private final ActorRef raftActor;
        private final String memberId;
            // FIXME: optionals? how about two different events?
        private final @Nullable String leaderId;
        private final short leaderPayloadVersion;

        public LeaderChanged(final ActorRef raftActor, final String memberId, final @Nullable String leaderId,
                final short leaderPayloadVersion) {
            this.raftActor = requireNonNull(raftActor);
            this.memberId = requireNonNull(memberId);
            this.leaderId = leaderId;
            this.leaderPayloadVersion = leaderPayloadVersion;
        }

        @Override
        public ActorRef raftActor() {
            return raftActor;
        }

        public final String memberId() {
            return memberId;
        }

        public final @Nullable String leaderId() {
            return leaderId;
        }

        public short leaderPayloadVersion() {
            return leaderPayloadVersion;
        }

        @Override
        public final String toString() {
            return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
        }

        protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return helper.add("memberId", memberId).add("leaderId", leaderId)
                .add("leaderPayloadVersion", leaderPayloadVersion);
        }
    }
}
