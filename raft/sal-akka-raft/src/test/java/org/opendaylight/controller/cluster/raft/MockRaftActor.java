/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.Support;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRaftActor extends RaftActor implements RaftActorRecoveryCohort, MockRaftActorSnapshotCohort {
    private static final Logger LOG = LoggerFactory.getLogger(MockRaftActor.class);

    public static final short PAYLOAD_VERSION = 5;

    final RaftActor actorDelegate;
    final RaftActorRecoveryCohort recoveryCohortDelegate;
    volatile MockRaftActorSnapshotCohort snapshotCohortDelegate;
    private final CountDownLatch recoveryComplete = new CountDownLatch(1);
    private final List<Object> state = Collections.synchronizedList(new ArrayList<>());
    private final ActorRef roleChangeNotifier;
    protected final CountDownLatch initializeBehaviorComplete = new CountDownLatch(1);
    private RaftActorSnapshotMessageSupport snapshotMessageSupport;
    private final Snapshot restoreFromSnapshot;
    private final Function<Runnable, Void> pauseLeaderFunction;

    protected MockRaftActor(final Path stateDir, final AbstractBuilder<?, ?> builder) {
        super(stateDir, builder.id, builder.peerAddresses != null ? builder.peerAddresses : Map.of(),
            Optional.ofNullable(builder.config), PAYLOAD_VERSION, AbstractActorTest.OBJECT_STREAMS);
        actorDelegate = mock(RaftActor.class);
        recoveryCohortDelegate = mock(RaftActorRecoveryCohort.class);

        if (builder.snapshotCohort == null) {
            snapshotCohortDelegate = mock(MockRaftActorSnapshotCohort.class);
            doCallRealMethod().when(snapshotCohortDelegate).support();
        } else {
            snapshotCohortDelegate = builder.snapshotCohort;
        }

        final var provider = builder.dataPersistenceProvider;
        if (provider == null) {
            setPersistence(builder.persistent.orElse(Boolean.TRUE));
        } else {
            persistence().decorateEntryStore((delegate, actor) -> provider.entryStore());
            persistence().decorateSnapshotStore((delegate, actor) -> provider.snapshotStore());
        }

        roleChangeNotifier = builder.roleChangeNotifier;
        snapshotMessageSupport = builder.snapshotMessageSupport;
        restoreFromSnapshot = builder.restoreFromSnapshot;
        pauseLeaderFunction = builder.pauseLeaderFunction;
    }

    @Override
    protected RaftActorSnapshotMessageSupport newRaftActorSnapshotMessageSupport() {
        if (snapshotMessageSupport == null) {
            snapshotMessageSupport = super.newRaftActorSnapshotMessageSupport();
        }
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
    protected void applyCommand(final Identifier identifier, final StateCommand command) {
        LOG.info("{}: applyState called: {}", memberId(), command);
        actorDelegate.applyCommand(identifier, command);
        state.add(command);
    }

    @Override
    protected RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
        return this;
    }

    @Override
    protected MockRaftActorSnapshotCohort getRaftActorSnapshotCohort() {
        return this;
    }

    @Override
    public void startLogRecoveryBatch(final int maxBatchSize) {
        // No-op
    }

    @Override
    public void appendRecoveredCommand(final StateCommand command) {
        state.add(command);
    }

    @Override
    public void applyCurrentLogRecoveryBatch() {
        // No-op
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
    public void applyRecoveredSnapshot(final StateSnapshot snapshot) {
        recoveryCohortDelegate.applyRecoveredSnapshot(snapshot);
        applySnapshotState(snapshot);
    }

    private void applySnapshotState(final StateSnapshot snapshot) {
        if (snapshot instanceof MockSnapshotState mockState) {
            state.clear();
            state.addAll(mockState.state());
        }
    }

    @Override
    public MockSnapshotState takeSnapshot() {
        return snapshotCohortDelegate.takeSnapshot();
    }

    @Override
    public void applySnapshot(final MockSnapshotState newState) {
        LOG.info("{}: applySnapshot called", memberId());
        applySnapshotState(newState);
        snapshotCohortDelegate.applySnapshot(newState);
    }

    @Override
    @NonNullByDefault
    public Support<MockSnapshotState> support() {
        LOG.info("{}: support() called", memberId());
        return snapshotCohortDelegate.support();
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
    @Deprecated
    protected void handleCommandImpl(final Object message) {
        if (message instanceof RaftActorBehavior msg) {
            super.changeCurrentBehavior(msg);
        } else {
            super.handleCommandImpl(message);
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
            return mockState.state();
        }

        throw new IllegalStateException("Unexpected snapshot State: " + from);
    }

    public final @Nullable SnapshotFile lastSnapshot() throws IOException {
        return persistence().snapshotStore().lastSnapshot();
    }

    public final @Nullable EntryJournal entryJournal() {
        return persistence().entryStore() instanceof EnabledRaftStorage enabled ? enabled.journal() : null;
    }

    @Override
    public Snapshot getRestoreFromSnapshot() {
        return restoreFromSnapshot;
    }

    public static Props props(final String id, final Path stateDir, final Map<String, String> peerAddresses,
            final ConfigParams config) {
        return builder().id(id).peerAddresses(peerAddresses).config(config).props(stateDir);
    }

    public static Props props(final String id, final Path stateDir, final Map<String, String> peerAddresses,
            final ConfigParams config, final PersistenceProvider dataPersistenceProvider) {
        return builder().id(id).peerAddresses(peerAddresses).config(config)
                .dataPersistenceProvider(dataPersistenceProvider).props(stateDir);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class AbstractBuilder<T extends AbstractBuilder<T, A>, A extends MockRaftActor> {
        private final Class<A> actorClass;
        private Map<String, String> peerAddresses = Map.of();
        private String id;
        private ConfigParams config;
        private PersistenceProvider dataPersistenceProvider;
        private ActorRef roleChangeNotifier;
        private RaftActorSnapshotMessageSupport snapshotMessageSupport;
        private Snapshot restoreFromSnapshot;
        private Optional<Boolean> persistent = Optional.empty();
        private Function<Runnable, Void> pauseLeaderFunction;
        private MockRaftActorSnapshotCohort snapshotCohort;

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

        public T dataPersistenceProvider(final PersistenceProvider newDataPersistenceProvider) {
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

        public T snapshotCohort(final MockRaftActorSnapshotCohort newSnapshotCohort) {
            snapshotCohort = requireNonNull(newSnapshotCohort);
            return self();
        }

        public Props props(final Path stateDir) {
            // FIXME: do not pass down builder, but rather values
            return Props.create(actorClass, stateDir, this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder, MockRaftActor> {
        Builder() {
            super(MockRaftActor.class);
        }
    }
}
