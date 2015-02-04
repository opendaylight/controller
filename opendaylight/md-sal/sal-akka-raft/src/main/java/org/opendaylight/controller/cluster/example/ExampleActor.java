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
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * A sample actor showing how the RaftActor is to be extended
 */
public class ExampleActor extends RaftActor {

    private final Map<String, String> state = new HashMap();
    private final DataPersistenceProvider dataPersistenceProvider;

    private long persistIdentifier = 1;
    private final Optional<ActorRef> roleChangeNotifier;


    public ExampleActor(String id, Map<String, String> peerAddresses,
        Optional<ConfigParams> configParams) {
        super(id, peerAddresses, configParams);
        this.dataPersistenceProvider = new PersistentDataProvider();
        roleChangeNotifier = createRoleChangeNotifier(id);
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams) {
        return Props.create(ExampleActor.class, id, peerAddresses, configParams);
    }

    @Override public void onReceiveCommand(Object message) throws Exception{
        if(message instanceof KeyValue){
            if(isLeader()) {
                String persistId = Long.toString(persistIdentifier++);
                persistData(getSender(), persistId, (Payload) message);
            } else {
                if(getLeader() != null) {
                    getLeader().forward(message, getContext());
                }
            }

        } else if (message instanceof PrintState) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("State of the node:{} has entries={}, {}",
                    getId(), state.size(), getReplicatedLogState());
            }

        } else if (message instanceof PrintRole) {
            if(LOG.isDebugEnabled()) {
                if (getRaftState() == RaftState.Leader || getRaftState() == RaftState.IsolatedLeader) {
                    final String followers = ((Leader)this.getCurrentBehavior()).printFollowerStates();
                    LOG.debug("{} = {}, Peers={}, followers={}", getId(), getRaftState(),
                        getRaftActorContext().getPeerAddresses().keySet(), followers);
                } else {
                    LOG.debug("{} = {}, Peers={}", getId(), getRaftState(),
                        getRaftActorContext().getPeerAddresses().keySet());
                }


            }

        } else {
            super.onReceiveCommand(message);
        }
    }

    protected String getReplicatedLogState() {
        return "snapshotIndex=" + getRaftActorContext().getReplicatedLog().getSnapshotIndex()
            + ", snapshotTerm=" + getRaftActorContext().getReplicatedLog().getSnapshotTerm()
            + ", im-mem journal size=" + getRaftActorContext().getReplicatedLog().size();
    }

    public Optional<ActorRef> createRoleChangeNotifier(String actorId) {
        ActorRef exampleRoleChangeNotifier = this.getContext().actorOf(
            RoleChangeNotifier.getProps(actorId), actorId + "-notifier");
        return Optional.<ActorRef>of(exampleRoleChangeNotifier);
    }

    @Override
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return roleChangeNotifier;
    }

    @Override protected void applyState(final ActorRef clientActor, final String identifier,
        final Object data) {
        if(data instanceof KeyValue){
            KeyValue kv = (KeyValue) data;
            state.put(kv.getKey(), kv.getValue());
            if(clientActor != null) {
                clientActor.tell(new KeyValueSaved(), getSelf());
            }
        }
    }

    @Override protected void createSnapshot() {
        ByteString bs = null;
        try {
            bs = fromObject(state);
        } catch (Exception e) {
            LOG.error(e, "Exception in creating snapshot");
        }
        getSelf().tell(new CaptureSnapshotReply(bs.toByteArray()), null);
    }

    @Override protected void applySnapshot(byte [] snapshot) {
        state.clear();
        try {
            state.putAll((HashMap) toObject(snapshot));
        } catch (Exception e) {
           LOG.error(e, "Exception in applying snapshot");
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("Snapshot applied to state : {}", ((HashMap) state).size());
        }
    }

    private ByteString fromObject(Object snapshot) throws Exception {
        ByteArrayOutputStream b = null;
        ObjectOutputStream o = null;
        try {
            b = new ByteArrayOutputStream();
            o = new ObjectOutputStream(b);
            o.writeObject(snapshot);
            byte[] snapshotBytes = b.toByteArray();
            return ByteString.copyFrom(snapshotBytes);
        } finally {
            if (o != null) {
                o.flush();
                o.close();
            }
            if (b != null) {
                b.close();
            }
        }
    }

    private Object toObject(byte [] bs) throws ClassNotFoundException, IOException {
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

    @Override protected void onStateChanged() {

    }

    @Override
    protected DataPersistenceProvider persistence() {
        return dataPersistenceProvider;
    }

    @Override public void onReceiveRecover(Object message)throws Exception {
        super.onReceiveRecover(message);
    }

    @Override public String persistenceId() {
        return getId();
    }

    @Override
    protected void startLogRecoveryBatch(int maxBatchSize) {
    }

    @Override
    protected void appendRecoveredLogEntry(Payload data) {
    }

    @Override
    protected void applyCurrentLogRecoveryBatch() {
    }

    @Override
    protected void onRecoveryComplete() {
    }

    @Override
    protected void applyRecoverySnapshot(byte[] snapshot) {
    }
}
