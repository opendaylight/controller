/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.UntypedPersistentActor;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton frontend actor. This actor is instantiated by ConcurrentDataBroker to handle coordination of that
 * broker's interactions with the backend (Shard and related classes).
 */
public final class FrontendActor extends UntypedPersistentActor {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendActor.class);
    private static final String PERSISTENCE_ID = "frontend actor";
    private State currentState;

    FrontendActor(final MemberName memberName) {
        currentState = new Recovering(memberName);
    }

    @Override
    public String persistenceId() {
        return PERSISTENCE_ID;
    }

    @Override
    public void onReceiveCommand(final Object command) throws Exception {
        currentState.onReceiveCommand(command);
    }

    @Override
    public void onReceiveRecover(final Object recover) throws Exception {
        currentState.onReceiveRecover(recover);
    }

    private static abstract class State {
        abstract void onReceiveCommand(Object command);
        abstract void onReceiveRecover(Object recover);
    }

    private static abstract class AbstractRecoveredState extends State {
        @Override
        final void onReceiveRecover(final Object recover) {
            throw new IllegalStateException("Frontend has been recovered");
        }
    }

    private final class Recovering extends State {
        private final MemberName currentName;
        private FrontendIdentifier lastId = null;

        Recovering(final MemberName memberName) {
            currentName = Preconditions.checkNotNull(memberName);
        }

        @Override
        void onReceiveCommand(final Object command) {
            throw new IllegalStateException("Frontend is recovering");
        }

        @Override
        void onReceiveRecover(final Object recover) {
            if (recover instanceof FrontendIdentifier) {
                lastId = (FrontendIdentifier) recover;
                LOG.debug("{}: recovered identifier {}", lastId);
            } else if (recover instanceof RecoveryCompleted) {
                final FrontendIdentifier nextId;
                if (lastId != null) {
                    Preconditions.checkState(currentName.equals(lastId.getMemberName()),
                        "Mismatched member name. Current: {} Saved: {}", currentName, lastId.getMemberName());

                    nextId = FrontendIdentifier.create(currentName, lastId.getGeneration() + 1);
                } else {
                    nextId = FrontendIdentifier.create(currentName, 0);
                }

                LOG.debug("{}: persisting new identifier {}", persistenceId(), nextId);
                saveSnapshot(nextId);
                currentState = new Saving(nextId);
            }
        }
    }

    private final class Saving extends AbstractRecoveredState {
        private final FrontendIdentifier myId;

        Saving(final FrontendIdentifier myId) {
            this.myId = Preconditions.checkNotNull(myId);
        }

        @Override
        void onReceiveCommand(Object command) {
            if (command instanceof SaveSnapshotSuccess) {
                currentState = new Operational(myId);
                LOG.debug("{}: transitioned to operational state", persistenceId());
            } else if (command instanceof SaveSnapshotFailure) {
                LOG.error("{}: failed to persist state", persistenceId(), ((SaveSnapshotFailure) command).cause());
                getSelf().tell(PoisonPill.getInstance(), getSelf());
            }
        }
    }

    private final class Operational extends AbstractRecoveredState {
        private final Map<Object, Request<?>> pendingReq = new HashMap<>();
        private final Collection<Request<?>> unsentReq = new ArrayList<>(1);
        private final FrontendIdentifier myId;
        private ActorRef shardLeader;
        private long txChainCounter;

        Operational(final FrontendIdentifier myId) {
            this.myId = Preconditions.checkNotNull(myId);
        }

        @Override
        void onReceiveCommand(final Object command) {

        }

        void createTransactionChain() {
            final LocalHistoryIdentifier id = new LocalHistoryIdentifier(myId, txChainCounter++);
            final CreateLocalHistoryRequest req = new CreateLocalHistoryRequest(getSelf(), id);

            if (shardLeader != null) {
                shardLeader.tell(req, req.getFrontendRef());
                pendingReq.put(id, req);
            } else {
                unsentReq.add(req);
            }
        }
    }
}
