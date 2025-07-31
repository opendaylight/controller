/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import com.google.common.collect.ImmutableMap;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Reader;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Writer;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample actor showing how the RaftActor is to be extended.
 */
@NonNullByDefault
public final class ExampleActor extends RaftActor
        implements RaftActorRecoveryCohort, RaftActorSnapshotCohort<ExampleActor.MapState>,
                   Support<ExampleActor.MapState> {
    private static final class PayloadIdentifier extends AbstractStringIdentifier<PayloadIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        PayloadIdentifier(final long identifier) {
            super(String.valueOf(identifier));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ExampleActor.class);
    private static final RestrictedObjectStreams OBJECT_STREAMS =
        RestrictedObjectStreams.ofClassLoaders(ExampleActor.class, RaftActor.class);

    private final HashMap<String, String> state = new HashMap<>();
    private final ActorRef roleChangeNotifier;

    private long persistIdentifier = 1;

    public ExampleActor(final Path stateDir, final String id, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams) {
        super(stateDir, id, peerAddresses, configParams, (short)0, OBJECT_STREAMS);
        setPersistence(true);
        roleChangeNotifier = getContext().actorOf(RoleChangeNotifier.getProps(id), id + "-notifier");
    }

    public static Props props(final Path stateDir, final String id, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams) {
        return Props.create(ExampleActor.class, stateDir, id, peerAddresses, configParams);
    }

    @Override
    protected void handleNonRaftCommand(final @Nullable Object message) {
        if (message instanceof KeyValue kv) {
            if (isLeader()) {
                submitCommand(new PayloadIdentifier(persistIdentifier++), kv, false);
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
                final var behavior = getCurrentBehavior();
                switch (behavior) {
                    case AbstractLeader leader -> {
                        LOG.debug("{} = {}, Peers={}, followers={}", memberId(), leader.raftRole(),
                            getRaftActorContext().getPeerIds(), leader.printFollowerStates());
                    }
                    default -> {
                        LOG.debug("{} = {}, Peers={}", memberId(), behavior.raftRole(),
                            getRaftActorContext().getPeerIds());
                    }
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
    protected @Nullable ActorRef roleChangeNotifier() {
        return roleChangeNotifier;
    }

    @Override
    public Class<MapState> snapshotType() {
        return MapState.class;
    }

    @Override
    protected void applyCommand(final @Nullable Identifier identifier, final StateCommand command) {
        if (command instanceof KeyValue kv) {
            state.put(kv.getKey(), kv.getValue());
        }
    }

    @Override
    public MapState takeSnapshot() {
        return new MapState(state);
    }

    @Override
    public void applySnapshot(final MapState snapshotState) {
        state.clear();
        state.putAll(snapshotState.state);

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
    public void appendRecoveredCommand(final StateCommand command) {
        // No-op
    }

    @Override
    public void applyCurrentLogRecoveryBatch() {
    }

    @Override
    public void onRecoveryComplete() {
    }

    @Override
    public void applyRecoveredSnapshot(final StateSnapshot snapshot) {
        // No-op
    }

    @Override
    protected RaftActorSnapshotCohort<MapState> getRaftActorSnapshotCohort() {
        return this;
    }

    @Override
    public @Nullable Snapshot getRestoreFromSnapshot() {
        return null;
    }

    @Override
    public Support<MapState> support() {
        return this;
    }

    @Override
    public Reader<MapState> reader() {
        return in -> new MapState(SerializationUtils.<Map<String, String>>deserialize(in.readAllBytes()));
    }

    @Override
    public Writer<MapState> writer() {
        return (snapshot, out) -> {
            try (var oos = new ObjectOutputStream(out)) {
                oos.writeObject(snapshot.state);
            }
        };
    }

    static class MapState implements Snapshot.State {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        final ImmutableMap<String, String> state;

        MapState(final Map<String, String> state) {
            this.state = ImmutableMap.copyOf(state);
        }
    }
}
