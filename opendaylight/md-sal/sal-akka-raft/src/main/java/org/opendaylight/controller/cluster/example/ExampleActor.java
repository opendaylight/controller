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
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

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
            LOG.debug("State of the node:{} has entries={}, {}",
                getId(), state.size(), getReplicatedLogState());

        } else if (message instanceof PrintRole) {
            LOG.debug("{} = {}, Peers={}", getId(), getRaftState(),getPeers());

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

    @Override protected Object createSnapshot() {
        return state;
    }

    @Override protected void applySnapshot(Object snapshot) {
        state.clear();
        state.putAll((HashMap) snapshot);
        LOG.debug("Snapshot applied to state :" + ((HashMap) snapshot).size());
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
