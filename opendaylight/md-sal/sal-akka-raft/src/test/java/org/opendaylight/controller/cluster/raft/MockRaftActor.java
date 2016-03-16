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
import com.google.common.base.Function;
import com.google.common.base.Optional;
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

    protected MockRaftActor(AbstractBuilder<?, ?> builder) {
        super(builder.id, builder.peerAddresses, Optional.fromNullable(builder.config), PAYLOAD_VERSION);
        state = new ArrayList<>();
        this.actorDelegate = mock(RaftActor.class);
        this.recoveryCohortDelegate = mock(RaftActorRecoveryCohort.class);
        this.snapshotCohortDelegate = mock(RaftActorSnapshotCohort.class);

        if(builder.dataPersistenceProvider == null){
            setPersistence(builder.persistent.isPresent() ? builder.persistent.get() : true);
        } else {
            setPersistence(builder.dataPersistenceProvider);
        }

        roleChangeNotifier = builder.roleChangeNotifier;
        snapshotMessageSupport = builder.snapshotMessageSupport;
        restoreFromSnapshot = builder.restoreFromSnapshot;
        pauseLeaderFunction = builder.pauseLeaderFunction;
    }

    public void setRaftActorRecoverySupport(RaftActorRecoverySupport support) {
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

    public RaftActorSnapshotMessageSupport getSnapshotMessageSupport() {
        return snapshotMessageSupport;
    }

    public void waitForRecoveryComplete() {
        try {
            assertEquals("Recovery complete", true, recoveryComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void waitForInitializeBehaviorComplete() {
        try {
            assertEquals("Behavior initialized", true, initializeBehaviorComplete.await(5,  TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void waitUntilLeader(){
        for(int i = 0;i < 10; i++){
            if(isLeader()){
                break;
            }
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
    }

    public List<Object> getState() {
        return state;
    }


    @Override protected void applyState(ActorRef clientActor, String identifier, Object data) {
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
    public void startLogRecoveryBatch(int maxBatchSize) {
    }

    @Override
    public void appendRecoveredLogEntry(Payload data) {
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
    public void applyRecoverySnapshot(byte[] bytes) {
        recoveryCohortDelegate.applyRecoverySnapshot(bytes);
        applySnapshotBytes(bytes);
    }

    private void applySnapshotBytes(byte[] bytes) {
        try {
            Object data = toObject(bytes);
            if (data instanceof List) {
                state.addAll((List<?>) data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createSnapshot(ActorRef actorRef) {
        LOG.info("{}: createSnapshot called", persistenceId());
        snapshotCohortDelegate.createSnapshot(actorRef);
    }

    @Override
    public void applySnapshot(byte [] snapshot) {
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

    protected void newBehavior(RaftActorBehavior newBehavior) {
        self().tell(newBehavior, ActorRef.noSender());
    }

    @Override
    protected void handleCommand(final Object message) {
        if(message instanceof RaftActorBehavior) {
            super.changeCurrentBehavior((RaftActorBehavior)message);
        } else {
            super.handleCommand(message);

            if(RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT.equals(message)) {
                snapshotCommitted.countDown();
            }
        }
    }

    @Override
    protected void pauseLeader(Runnable operation) {
        if(pauseLeaderFunction != null) {
            pauseLeaderFunction.apply(operation);
        } else {
            super.pauseLeader(operation);
        }
    }

    public static Object toObject(byte[] bs) throws ClassNotFoundException, IOException {
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

    public ReplicatedLog getReplicatedLog(){
        return this.getRaftActorContext().getReplicatedLog();
    }

    @Override
    public byte[] getRestoreFromSnapshot() {
        return restoreFromSnapshot;
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
            ConfigParams config){
        return builder().id(id).peerAddresses(peerAddresses).config(config).props();
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
                              ConfigParams config, DataPersistenceProvider dataPersistenceProvider){
        return builder().id(id).peerAddresses(peerAddresses).config(config).
                dataPersistenceProvider(dataPersistenceProvider).props();
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

        protected AbstractBuilder(Class<A> actorClass) {
            this.actorClass = actorClass;
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }

        public T id(String id) {
            this.id = id;
            return self();
        }

        public T peerAddresses(Map<String, String> peerAddresses) {
            this.peerAddresses = peerAddresses;
            return self();
        }

        public T config(ConfigParams config) {
            this.config = config;
            return self();
        }

        public T dataPersistenceProvider(DataPersistenceProvider dataPersistenceProvider) {
            this.dataPersistenceProvider = dataPersistenceProvider;
            return self();
        }

        public T roleChangeNotifier(ActorRef roleChangeNotifier) {
            this.roleChangeNotifier = roleChangeNotifier;
            return self();
        }

        public T snapshotMessageSupport(RaftActorSnapshotMessageSupport snapshotMessageSupport) {
            this.snapshotMessageSupport = snapshotMessageSupport;
            return self();
        }

        public T restoreFromSnapshot(byte[] restoreFromSnapshot) {
            this.restoreFromSnapshot = restoreFromSnapshot;
            return self();
        }

        public T persistent(Optional<Boolean> persistent) {
            this.persistent = persistent;
            return self();
        }

        public T pauseLeaderFunction(Function<Runnable, Void> pauseLeaderFunction) {
            this.pauseLeaderFunction = pauseLeaderFunction;
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
