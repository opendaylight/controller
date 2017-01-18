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

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
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
    private final byte[] restoreFromSnapshot;
    final CountDownLatch snapshotCommitted = new CountDownLatch(1);
    private final Function<Runnable, Void> pauseLeaderFunction;

    protected MockRaftActor(final AbstractBuilder<?, ?> builder) {
        super(builder.id, builder.peerAddresses != null ? builder.peerAddresses :
            Collections.<String, String>emptyMap(), Optional.fromNullable(builder.config), PAYLOAD_VERSION);
        state = new ArrayList<>();
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
            assertTrue("Recovery complete", recoveryComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }

    public void waitForInitializeBehaviorComplete() {
        try {
            assertTrue("Behavior initialized", initializeBehaviorComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Throwables.propagate(e);
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
    @Nonnull
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
    public void applyRecoverySnapshot(final byte[] bytes) {
        recoveryCohortDelegate.applyRecoverySnapshot(bytes);
        applySnapshotBytes(bytes);
    }

    private void applySnapshotBytes(final byte[] bytes) {
        if (bytes.length == 0) {
            return;
        }

        try {
            Object data = toObject(bytes);
            if (data instanceof List) {
                state.clear();
                state.addAll((List<?>) data);
            }
        } catch (ClassNotFoundException | IOException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void createSnapshot(final ActorRef actorRef) {
        LOG.info("{}: createSnapshot called", persistenceId());
        snapshotCohortDelegate.createSnapshot(actorRef);
    }

    @Override
    public void applySnapshot(final byte [] snapshot) {
        LOG.info("{}: applySnapshot called", persistenceId());
        applySnapshotBytes(snapshot);
        snapshotCohortDelegate.applySnapshot(snapshot);
    }

    @Override
    protected void onStateChanged() {
        actorDelegate.onStateChanged();
    }

    @Override
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return Optional.fromNullable(roleChangeNotifier);
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

    public static Object toObject(final byte[] bs) throws ClassNotFoundException, IOException {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bs);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return obj;
    }

    public ReplicatedLog getReplicatedLog() {
        return this.getRaftActorContext().getReplicatedLog();
    }

    @Override
    public byte[] getRestoreFromSnapshot() {
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
        private byte[] restoreFromSnapshot;
        private Optional<Boolean> persistent = Optional.absent();
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

        public T restoreFromSnapshot(final byte[] newRestoreFromSnapshot) {
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
        private Builder() {
            super(MockRaftActor.class);
        }
    }
}
