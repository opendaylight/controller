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
import akka.japi.Creator;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

public class MockRaftActor extends RaftActor implements RaftActorRecoveryCohort, RaftActorSnapshotCohort {

    final RaftActor actorDelegate;
    final RaftActorRecoveryCohort recoveryCohortDelegate;
    final RaftActorSnapshotCohort snapshotCohortDelegate;
    private final CountDownLatch recoveryComplete = new CountDownLatch(1);
    private final List<Object> state;
    private ActorRef roleChangeNotifier;
    private final CountDownLatch initializeBehaviorComplete = new CountDownLatch(1);
    private RaftActorRecoverySupport raftActorRecoverySupport;

    public static final class MockRaftActorCreator implements Creator<MockRaftActor> {
        private static final long serialVersionUID = 1L;
        private final Map<String, String> peerAddresses;
        private final String id;
        private final Optional<ConfigParams> config;
        private final DataPersistenceProvider dataPersistenceProvider;
        private final ActorRef roleChangeNotifier;

        private MockRaftActorCreator(Map<String, String> peerAddresses, String id,
            Optional<ConfigParams> config, DataPersistenceProvider dataPersistenceProvider,
            ActorRef roleChangeNotifier) {
            this.peerAddresses = peerAddresses;
            this.id = id;
            this.config = config;
            this.dataPersistenceProvider = dataPersistenceProvider;
            this.roleChangeNotifier = roleChangeNotifier;
        }

        @Override
        public MockRaftActor create() throws Exception {
            MockRaftActor mockRaftActor = new MockRaftActor(id, peerAddresses, config,
                dataPersistenceProvider);
            mockRaftActor.roleChangeNotifier = this.roleChangeNotifier;
            return mockRaftActor;
        }
    }

    public MockRaftActor(String id, Map<String, String> peerAddresses, Optional<ConfigParams> config,
                         DataPersistenceProvider dataPersistenceProvider) {
        super(id, peerAddresses, config);
        state = new ArrayList<>();
        this.actorDelegate = mock(RaftActor.class);
        this.recoveryCohortDelegate = mock(RaftActorRecoveryCohort.class);
        this.snapshotCohortDelegate = mock(RaftActorSnapshotCohort.class);
        if(dataPersistenceProvider == null){
            setPersistence(true);
        } else {
            setPersistence(dataPersistenceProvider);
        }
    }

    public void setRaftActorRecoverySupport(RaftActorRecoverySupport support) {
        raftActorRecoverySupport = support;
    }

    @Override
    public RaftActorRecoverySupport newRaftActorRecoverySupport() {
        return raftActorRecoverySupport != null ? raftActorRecoverySupport : super.newRaftActorRecoverySupport();
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

    public static Props props(final String id, final Map<String, String> peerAddresses,
            Optional<ConfigParams> config){
        return Props.create(new MockRaftActorCreator(peerAddresses, id, config, null, null));
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
                              Optional<ConfigParams> config, DataPersistenceProvider dataPersistenceProvider){
        return Props.create(new MockRaftActorCreator(peerAddresses, id, config, dataPersistenceProvider, null));
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
        Optional<ConfigParams> config, ActorRef roleChangeNotifier){
        return Props.create(new MockRaftActorCreator(peerAddresses, id, config, null, roleChangeNotifier));
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
                              Optional<ConfigParams> config, ActorRef roleChangeNotifier,
                              DataPersistenceProvider dataPersistenceProvider){
        return Props.create(new MockRaftActorCreator(peerAddresses, id, config, dataPersistenceProvider, roleChangeNotifier));
    }


    @Override protected void applyState(ActorRef clientActor, String identifier, Object data) {
        actorDelegate.applyState(clientActor, identifier, data);
        LOG.info("{}: applyState called", persistenceId());
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

    private Object toObject(byte[] bs) throws ClassNotFoundException, IOException {
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
}