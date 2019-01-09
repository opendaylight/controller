/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

/**
 * A sample actor showing how the RaftActor is to be extended.
 */
public class ExampleActor extends RaftActor implements RaftActorRecoveryCohort, RaftActorSnapshotCohort {
    private static final class PayloadIdentifier extends AbstractStringIdentifier<PayloadIdentifier> {
        private static final long serialVersionUID = 1L;

        PayloadIdentifier(final long identifier) {
            super(String.valueOf(identifier));
        }
    }

    private final Map<String, String> state = new HashMap<>();
    private final Optional<ActorRef> roleChangeNotifier;

    private long persistIdentifier = 1;

    public ExampleActor(final String id, final Map<String, String> peerAddresses,
            finalOptional<ConfigParams> configParams) {
        super(id, peerAddresses, configParams, (short)0, false);
        setPersistence(true);
        roleChangeNotifier = createRoleChangeNotifier(id);
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams) {
        return Props.create(ExampleActor.class, id, peerAddresses, configParams);
    }

    @Override
    protected void handleNonRaftCommand(final Object message) {
        if (message instanceof KeyValue) {
            if (isLeader()) {
                persistData(getSender(), new PayloadIdentifier(persistIdentifier++), (Payload) message, false);
            } else {
                if (getLeader() != null) {
                    getLeader().forward(message, getContext());
                }
            }

        } else if (message instanceof PrintState) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("State of the node:{} has entries={}, {}",
                    getId(), state.size(), getReplicatedLogState());
            }

        } else if (message instanceof PrintRole) {
            if (LOG.isDebugEnabled()) {
                if (getRaftState() == RaftState.Leader || getRaftState() == RaftState.IsolatedLeader) {
                    final String followers = ((Leader)this.getCurrentBehavior()).printFollowerStates();
                    LOG.debug("{} = {}, Peers={}, followers={}", getId(), getRaftState(),
                        getRaftActorContext().getPeerIds(), followers);
                } else {
                    LOG.debug("{} = {}, Peers={}", getId(), getRaftState(),
                        getRaftActorContext().getPeerIds());
                }


            }

        } else {
            super.handleNonRaftCommand(message);
        }
    }

    protected String getReplicatedLogState() {
        return "snapshotIndex=" + getRaftActorContext().getReplicatedLog().getSnapshotIndex()
            + ", snapshotTerm=" + getRaftActorContext().getReplicatedLog().getSnapshotTerm()
            + ", im-mem journal size=" + getRaftActorContext().getReplicatedLog().size();
    }

    public Optional<ActorRef> createRoleChangeNotifier(final String actorId) {
        ActorRef exampleRoleChangeNotifier = this.getContext().actorOf(
            RoleChangeNotifier.getProps(actorId), actorId + "-notifier");
        return Optional.<ActorRef>of(exampleRoleChangeNotifier);
    }

    @Override
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return roleChangeNotifier;
    }

    @Override
    protected void applyState(final ActorRef clientActor, final Identifier identifier, final Object data) {
        if (data instanceof KeyValue) {
            KeyValue kv = (KeyValue) data;
            state.put(kv.getKey(), kv.getValue());
            if (clientActor != null) {
                clientActor.tell(new KeyValueSaved(), getSelf());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
        try {
            if (installSnapshotStream.isPresent()) {
                SerializationUtils.serialize((Serializable) state, installSnapshotStream.get());
            }
        } catch (RuntimeException e) {
            LOG.error("Exception in creating snapshot", e);
        }

        getSelf().tell(new CaptureSnapshotReply(new MapState(state), installSnapshotStream), null);
    }

    @Override
    public void applySnapshot(final Snapshot.State snapshotState) {
        state.clear();
        state.putAll(((MapState)snapshotState).state);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Snapshot applied to state : {}", state.size());
        }
    }

    @Override
    protected void onStateChanged() {

    }

    @Override
    public String persistenceId() {
        return getId();
    }

    @Override
    protected RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
        return this;
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
    }

    @Override
    public void appendRecoveredLogEntry(final Payload data) {
    }

    @Override
    public void applyCurrentLogRecoveryBatch() {
    }

    @Override
    public void onRecoveryComplete() {
    }

    @Override
    public void applyRecoverySnapshot(final Snapshot.State snapshotState) {
    }

    @Override
    protected RaftActorSnapshotCohort getRaftActorSnapshotCohort() {
        return this;
    }

    @Override
    public Snapshot getRestoreFromSnapshot() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Snapshot.State deserializeSnapshot(final ByteSource snapshotBytes) {
        try {
            return new MapState((Map<String, String>) SerializationUtils.deserialize(snapshotBytes.read()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MapState implements Snapshot.State {
        private static final long serialVersionUID = 1L;

        Map<String, String> state;

        MapState(final Map<String, String> state) {
            this.state = state;
        }
    }
}
