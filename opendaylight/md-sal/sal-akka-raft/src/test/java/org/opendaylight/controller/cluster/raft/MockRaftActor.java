/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;

public class MockRaftActor extends RaftActor implements RaftActorRecoveryCohort, RaftActorSnapshotCohort {
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

    protected MockRaftActor(final AbstractBuilder<?, ?> builder) {
        super(builder.id, builder.peerAddresses != null ? builder.peerAddresses :
            Collections.emptyMap(), Optional.ofNullable(builder.config), PAYLOAD_VERSION, false);
        state = Collections.synchronizedList(new ArrayList<>());
        this.actorDelegate = mock(RaftActor.class);
        this.recoveryCohortDelegate = mock(RaftActorRecoveryCohort.class);

        this.snapshotCohortDelegate = builder.snapshotCohort != null ? builder.snapshotCohort :
            mock(RaftActorSnapshotCohort.class);

        if (builder.dataPersistenceProvider == null) {
            setPersistence(builder.persistent.isPresent() ? builder.persistent.get() : true);
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
    public RaftActorRecoverySupport newRaftActorRecoverySupport() {
        return raftActorRecoverySupport != null ? raftActorRecoverySupport : super.newRaftActorRecoverySupport();
    }

    @Override
    protected RaftActorSnapshotMessageSupport newRaftActorSnapshotMessageSupport() {
        return snapshotMessageSupport != null ? snapshotMessageSupport :
            (snapshotMessageSupport = super.newRaftActorSnapshotMessageSupport());
    }

    @Override
    public RaftActorContext getRaftActorContext() {
        return super.getRaftActorContext();
    }

    public RaftActorSnapshotMessageSupport getSnapshotMessageSupport() {
        return snapshotMessageSupport;
    }

    public void waitForRecoveryComplete() {
        try {
            assertEquals("Recovery complete", true, recoveryComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForInitializeBehaviorComplete() {
        try {
            assertEquals("Behavior initialized", true, initializeBehaviorComplete.await(5,  TimeUnit.SECONDS));
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
        LOG.info("{}: applyState called: {}", persistenceId(), data);

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
    protected void initializeBehavior() {
        super.initializeBehavior();
        initializeBehaviorComplete.countDown();
    }

    @Override
    public void applyRecoverySnapshot(final Snapshot.State newState) {
        recoveryCohortDelegate.applyRecoverySnapshot(newState);
        applySnapshotState(newState);
    }

    private void applySnapshotState(final Snapshot.State newState) {
        if (newState instanceof MockSnapshotState) {
            state.clear();
            state.addAll(((MockSnapshotState)newState).getState());
        }
    }

    @Override
    public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
        LOG.info("{}: createSnapshot called", persistenceId());
        snapshotCohortDelegate.createSnapshot(actorRef, installSnapshotStream);
    }

    @Override
    public void applySnapshot(final Snapshot.State newState) {
        LOG.info("{}: applySnapshot called", persistenceId());
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
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return Optional.ofNullable(roleChangeNotifier);
    }

    @Override public String persistenceId() {
        return this.getId();
    }

    protected void newBehavior(final RaftActorBehavior newBehavior) {
        self().tell(newBehavior, ActorRef.noSender());
    }

    @Override
    protected void handleCommand(final Object message) {
        if (message instanceof RaftActorBehavior) {
            super.changeCurrentBehavior((RaftActorBehavior)message);
        } else {
            super.handleCommand(message);

            if (RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT.equals(message)) {
                snapshotCommitted.countDown();
            }
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
        if (from instanceof MockSnapshotState) {
            return ((MockSnapshotState)from).getState();
        }

        throw new IllegalStateException("Unexpected snapshot State: " + from);
    }

    public ReplicatedLog getReplicatedLog() {
        return this.getRaftActorContext().getReplicatedLog();
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
            this.id = newId;
            return self();
        }

        public T peerAddresses(final Map<String, String> newPeerAddresses) {
            this.peerAddresses = newPeerAddresses;
            return self();
        }

        public T config(final ConfigParams newConfig) {
            this.config = newConfig;
            return self();
        }

        public T dataPersistenceProvider(final DataPersistenceProvider newDataPersistenceProvider) {
            this.dataPersistenceProvider = newDataPersistenceProvider;
            return self();
        }

        public T roleChangeNotifier(final ActorRef newRoleChangeNotifier) {
            this.roleChangeNotifier = newRoleChangeNotifier;
            return self();
        }

        public T snapshotMessageSupport(final RaftActorSnapshotMessageSupport newSnapshotMessageSupport) {
            this.snapshotMessageSupport = newSnapshotMessageSupport;
            return self();
        }

        public T restoreFromSnapshot(final Snapshot newRestoreFromSnapshot) {
            this.restoreFromSnapshot = newRestoreFromSnapshot;
            return self();
        }

        public T persistent(final Optional<Boolean> newPersistent) {
            this.persistent = newPersistent;
            return self();
        }

        public T pauseLeaderFunction(final Function<Runnable, Void> newPauseLeaderFunction) {
            this.pauseLeaderFunction = newPauseLeaderFunction;
            return self();
        }

        public T snapshotCohort(final RaftActorSnapshotCohort newSnapshotCohort) {
            this.snapshotCohort = newSnapshotCohort;
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
            final int prime = 31;
            int result = 1;
            result = prime * result + (state == null ? 0 : state.hashCode());
            return result;
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
            if (state == null) {
                if (other.state != null) {
                    return false;
                }
            } else if (!state.equals(other.state)) {
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
