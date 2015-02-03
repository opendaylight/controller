/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.programs.appendentries;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

public class Server {

    private static ActorSystem actorSystem;

    public static class ServerActor extends UntypedActor {

        @Override public void onReceive(Object message) throws Exception {
            if(AppendEntries.LEGACY_SERIALIZABLE_CLASS.equals(message.getClass())){
                AppendEntries appendEntries =
                    AppendEntries.fromSerializable(message);

                Payload data = appendEntries.getEntries()
                    .get(0).getData();
                if(data instanceof KeyValue){
                    System.out.println("Received : " + ((KeyValue) appendEntries.getEntries().get(0).getData()).getKey());
                } else {
                    System.out.println("Received :" +
                        ((CompositeModificationPayload) appendEntries
                            .getEntries()
                            .get(0).getData()).getModification().toString());
                }
            } else if(message instanceof String){
                System.out.println(message);
            }
        }
    }

    public static void main(String[] args){
        actorSystem = ActorSystem.create("appendentries", ConfigFactory
            .load().getConfig("ODLCluster"));

        actorSystem.actorOf(Props.create(ServerActor.class), "server");
    }
}
