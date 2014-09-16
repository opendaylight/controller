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
import akka.japi.Creator;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A sample actor showing how the RaftActor is to be extended
 */
public class ExampleActor extends RaftActor {

    private final Map<String, String> state = new HashMap();

    private long persistIdentifier = 1;


    public ExampleActor(String id, Map<String, String> peerAddresses,
        Optional<ConfigParams> configParams) {
        super(id, peerAddresses, configParams);
    }

    public static Props props(final String id, final Map<String, String> peerAddresses,
        final Optional<ConfigParams> configParams){
        return Props.create(new Creator<ExampleActor>(){

            @Override public ExampleActor create() throws Exception {
                return new ExampleActor(id, peerAddresses, configParams);
            }
        });
    }

    @Override public void onReceiveCommand(Object message){
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
                LOG.debug("{} = {}, Peers={}", getId(), getRaftState(), getPeers());
            }

        } else {
            super.onReceiveCommand(message);
        }
    }

    @Override protected void applyState(ActorRef clientActor, String identifier,
        Object data) {
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
            LOG.error("Exception in creating snapshot", e);
        }
        getSelf().tell(new CaptureSnapshotReply(bs), null);
    }

    @Override protected void applySnapshot(ByteString snapshot) {
        state.clear();
        try {
            state.putAll((HashMap) toObject(snapshot));
        } catch (Exception e) {
           LOG.error("Exception in applying snapshot", e);
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("Snapshot applied to state :" + ((HashMap) state).size());
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

    private Object toObject(ByteString bs) throws ClassNotFoundException, IOException {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bs.toByteArray());
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

    @Override public void onReceiveRecover(Object message) {
        super.onReceiveRecover(message);
    }

    @Override public String persistenceId() {
        return getId();
    }
}
