/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
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
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample actor showing how the RaftActor is to be extended.
 */
public final class ExampleActor extends RaftActor implements RaftActorRecoveryCohort, RaftActorSnapshotCohort {
    private static final class PayloadIdentifier extends AbstractStringIdentifier<PayloadIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        PayloadIdentifier(final long identifier) {
            super(String.valueOf(identifier));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ExampleActor.class);

    private final Map<String, String> state = new HashMap<>();
    private final ActorRef roleChangeNotifier;

    private long persistIdentifier = 1;

    public ExampleActor(final Path stateDir, final String id, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams) {
        super(stateDir, id, peerAddresses, configParams, (short)0);
        setPersistence(true);
        roleChangeNotifier = getContext().actorOf(RoleChangeNotifier.getProps(id), id + "-notifier");
    }

    public static Props props(final Path stateDir, final String id, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams) {
        return Props.create(ExampleActor.class, stateDir, id, peerAddresses, configParams);
    }

    @Override
    protected void handleNonRaftCommand(final Object message) {
        if (message instanceof KeyValue kv) {
            if (isLeader()) {
                persistData(getSender(), new PayloadIdentifier(persistIdentifier++), kv, false);
            } else if (getLeader() != null) {
                getLeader().forward(message, getContext());
            }

        } else if (message instanceof PrintState) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("State of the node:{} has entries={}, {}",
                    memberId(), state.size(), getReplicatedLogState());
            }

        } else if (message instanceof PrintRole) {
            if (LOG.isDebugEnabled()) {
                if (getRaftState() == RaftState.Leader || getRaftState() == RaftState.IsolatedLeader) {
                    final String followers = ((Leader)getCurrentBehavior()).printFollowerStates();
                    LOG.debug("{} = {}, Peers={}, followers={}", memberId(), getRaftState(),
                        getRaftActorContext().getPeerIds(), followers);
                } else {
                    LOG.debug("{} = {}, Peers={}", memberId(), getRaftState(),
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

    @Override
    protected ActorRef roleChangeNotifier() {
        return roleChangeNotifier;
    }

    @Override
    protected void applyState(final ActorRef clientActor, final Identifier identifier, final Object data) {
        if (data instanceof KeyValue kv) {
            state.put(kv.getKey(), kv.getValue());
            if (clientActor != null) {
                clientActor.tell(new KeyValueSaved(), self());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
        try {
            if (installSnapshotStream.isPresent()) {
                SerializationUtils.serialize((Serializable) state, installSnapshotStream.orElseThrow());
            }
        } catch (RuntimeException e) {
            LOG.error("Exception in creating snapshot", e);
        }

        self().tell(new CaptureSnapshotReply(new MapState(state), installSnapshotStream.orElse(null)), null);
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
            throw new IllegalStateException(e);
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
