/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRaftActor extends RaftActor implements RaftActorRecoveryCohort, RaftActorSnapshotCohort {
    private static final Logger LOG = LoggerFactory.getLogger(MockRaftActor.class);

    public static final short PAYLOAD_VERSION = 5;

    final RaftActor actorDelegate;
    final RaftActorRecoveryCohort recoveryCohortDelegate;
    volatile RaftActorSnapshotCohort snapshotCohortDelegate;
    private final CountDownLatch recoveryComplete = new CountDownLatch(1);
    private final List<Object> state;
    private final ActorRef roleChangeNotifier;
    protected final CountDownLatch initializeBehaviorComplete = new CountDownLatch(1);
    private RaftActorRecoverySupport raftActorRecoverySupport;
    private RaftActorSnapshotMessageSupport snapshotMessageSupport;
    private final Snapshot restoreFromSnapshot;
    final CountDownLatch snapshotCommitted = new CountDownLatch(1);
    private final Function<Runnable, Void> pauseLeaderFunction;

    protected MockRaftActor(final Path basePath, final AbstractBuilder<?, ?> builder) {
        super(basePath, builder.id, builder.peerAddresses != null ? builder.peerAddresses : Map.of(),
            Optional.ofNullable(builder.config), PAYLOAD_VERSION);
        state = Collections.synchronizedList(new ArrayList<>());
        actorDelegate = mock(RaftActor.class);
        recoveryCohortDelegate = mock(RaftActorRecoveryCohort.class);

        snapshotCohortDelegate = builder.snapshotCohort != null ? builder.snapshotCohort :
            mock(RaftActorSnapshotCohort.class);

        if (builder.dataPersistenceProvider == null) {
            setPersistence(builder.persistent.isPresent() ? builder.persistent.orElseThrow() : true);
        } else {
            setPersistence(builder.dataPersistenceProvider);
        }

        roleChangeNotifier = builder.roleChangeNotifier;
        snapshotMessageSupport = builder.snapshotMessageSupport;
        restoreFromSnapshot = builder.restoreFromSnapshot;
        pauseLeaderFunction = builder.pauseLeaderFunction;
    }

    public void setRaftActorRecoverySupport(final RaftActorRecoverySupport support) {
        raftActorRecoverySupport = support;
    }

    @Override
    RaftActorRecoverySupport newRaftActorRecoverySupport() {
        return raftActorRecoverySupport != null ? raftActorRecoverySupport : super.newRaftActorRecoverySupport();
    }

    @Override
    protected RaftActorSnapshotMessageSupport newRaftActorSnapshotMessageSupport() {
        return snapshotMessageSupport != null ? snapshotMessageSupport :
            (snapshotMessageSupport = super.newRaftActorSnapshotMessageSupport());
    }

    public RaftActorSnapshotMessageSupport getSnapshotMessageSupport() {
        return snapshotMessageSupport;
    }

    public void waitForRecoveryComplete() {
        try {
            assertTrue("Recovery complete", recoveryComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForInitializeBehaviorComplete() {
        try {
            assertTrue("Behavior initialized", initializeBehaviorComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public void waitUntilLeader() {
        for (int i = 0; i < 10; i++) {
            if (isLeader()) {
                break;
            }
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
    }

    public List<Object> getState() {
        return state;
    }

    @Override
    protected void applyState(final ActorRef clientActor, final Identifier identifier, final Object data) {
        actorDelegate.applyState(clientActor, identifier, data);
        LOG.info("{}: applyState called: {}", memberId(), data);

        state.add(data);
    }

    @Override
    protected RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
        return this;
    }

    @Override
    protected RaftActorSnapshotCohort getRaftActorSnapshotCohort() {
        return this;
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
    }

    @Override
    public void appendRecoveredLogEntry(final Payload data) {
        state.add(data);
    }

    @Override
    public void applyCurrentLogRecoveryBatch() {
    }

    @Override
    protected void onRecoveryComplete() {
        actorDelegate.onRecoveryComplete();
        recoveryComplete.countDown();
    }

    @Override
    void initializeBehavior() {
        super.initializeBehavior();
        initializeBehaviorComplete.countDown();
    }

    @Override
    public void applyRecoverySnapshot(final Snapshot.State newState) {
        recoveryCohortDelegate.applyRecoverySnapshot(newState);
        applySnapshotState(newState);
    }

    private void applySnapshotState(final Snapshot.State newState) {
        if (newState instanceof MockSnapshotState mockState) {
            state.clear();
            state.addAll(mockState.getState());
        }
    }

    @Override
    public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
        LOG.info("{}: createSnapshot called", memberId());
        snapshotCohortDelegate.createSnapshot(actorRef, installSnapshotStream);
    }

    @Override
    public void applySnapshot(final Snapshot.State newState) {
        LOG.info("{}: applySnapshot called", memberId());
        applySnapshotState(newState);
        snapshotCohortDelegate.applySnapshot(newState);
    }

    @Override
    public Snapshot.State deserializeSnapshot(final ByteSource snapshotBytes) {
        try {
            return (Snapshot.State) SerializationUtils.deserialize(snapshotBytes.read());
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing state", e);
        }
    }

    @Override
    protected void onStateChanged() {
        actorDelegate.onStateChanged();
    }

    @Override
    protected ActorRef roleChangeNotifier() {
        return roleChangeNotifier;
    }

    protected void newBehavior(final RaftActorBehavior newBehavior) {
        self().tell(newBehavior, ActorRef.noSender());
    }

    @Override
    protected void handleCommand(final Object message) {
        if (message instanceof RaftActorBehavior msg) {
            super.changeCurrentBehavior(msg);
            return;
        }

        super.handleCommand(message);
        if (message instanceof RaftActorSnapshotMessageSupport.CommitSnapshot) {
            snapshotCommitted.countDown();
        }
    }

    @Override
    protected void pauseLeader(final Runnable operation) {
        if (pauseLeaderFunction != null) {
            pauseLeaderFunction.apply(operation);
        } else {
            super.pauseLeader(operation);
        }
    }

    public static List<Object> fromState(final Snapshot.State from) {
        if (from instanceof MockSnapshotState mockState) {
            return mockState.getState();
        }

        throw new IllegalStateException("Unexpected snapshot State: " + from);
    }

    public ReplicatedLog getReplicatedLog() {
        return getRaftActorContext().getReplicatedLog();
    }

    @Override
    public Snapshot getRestoreFromSnapshot() {
        return restoreFromSnapshot;
    }

    public static Props props(final String id, final Map<String, String> peerAddresses, final ConfigParams config) {
        return builder().id(id).peerAddresses(peerAddresses).config(config).props();
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
                              final ConfigParams config, final DataPersistenceProvider dataPersistenceProvider) {
        return builder().id(id).peerAddresses(peerAddresses).config(config)
                .dataPersistenceProvider(dataPersistenceProvider).props();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class AbstractBuilder<T extends AbstractBuilder<T, A>, A extends MockRaftActor> {
        private Map<String, String> peerAddresses = Collections.emptyMap();
        private String id;
        private ConfigParams config;
        private DataPersistenceProvider dataPersistenceProvider;
        private ActorRef roleChangeNotifier;
        private RaftActorSnapshotMessageSupport snapshotMessageSupport;
        private Snapshot restoreFromSnapshot;
        private Optional<Boolean> persistent = Optional.empty();
        private final Class<A> actorClass;
        private Function<Runnable, Void> pauseLeaderFunction;
        private RaftActorSnapshotCohort snapshotCohort;

        protected AbstractBuilder(final Class<A> actorClass) {
            this.actorClass = actorClass;
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }

        public T id(final String newId) {
            id = newId;
            return self();
        }

        public T peerAddresses(final Map<String, String> newPeerAddresses) {
            peerAddresses = newPeerAddresses;
            return self();
        }

        public T config(final ConfigParams newConfig) {
            config = newConfig;
            return self();
        }

        public T dataPersistenceProvider(final DataPersistenceProvider newDataPersistenceProvider) {
            dataPersistenceProvider = newDataPersistenceProvider;
            return self();
        }

        public T roleChangeNotifier(final ActorRef newRoleChangeNotifier) {
            roleChangeNotifier = newRoleChangeNotifier;
            return self();
        }

        public T snapshotMessageSupport(final RaftActorSnapshotMessageSupport newSnapshotMessageSupport) {
            snapshotMessageSupport = newSnapshotMessageSupport;
            return self();
        }

        public T restoreFromSnapshot(final Snapshot newRestoreFromSnapshot) {
            restoreFromSnapshot = newRestoreFromSnapshot;
            return self();
        }

        public T persistent(final Optional<Boolean> newPersistent) {
            persistent = newPersistent;
            return self();
        }

        public T pauseLeaderFunction(final Function<Runnable, Void> newPauseLeaderFunction) {
            pauseLeaderFunction = newPauseLeaderFunction;
            return self();
        }

        public T snapshotCohort(final RaftActorSnapshotCohort newSnapshotCohort) {
            snapshotCohort = newSnapshotCohort;
            return self();
        }

        public Props props() {
            return Props.create(actorClass, this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder, MockRaftActor> {
        Builder() {
            super(MockRaftActor.class);
        }
    }

    public static class MockSnapshotState implements Snapshot.State {
        private static final long serialVersionUID = 1L;

        private final List<Object> state;

        public MockSnapshotState(final List<Object> state) {
            this.state = state;
        }

        public List<Object> getState() {
            return state;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MockSnapshotState other = (MockSnapshotState) obj;
            if (!Objects.equals(state, other.state)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "MockSnapshotState [state=" + state + "]";
        }
    }
}
