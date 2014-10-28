/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.programs.appendentries;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import java.util.ArrayList;
import java.util.List;

public class Client {

    private static ActorSystem actorSystem;

    public static class ClientActor extends UntypedActor {

        @Override public void onReceive(Object message) throws Exception {

        }
    }

    public static void main(String[] args){
        actorSystem = ActorSystem.create("appendentries", ConfigFactory
            .load().getConfig("ODLCluster"));

        ActorSelection actorSelection = actorSystem.actorSelection(
            "akka.tcp://appendentries@127.0.0.1:2550/user/server");

        AppendEntries appendEntries = modificationAppendEntries();

        Payload data = appendEntries.getEntries().get(0).getData();
        if(data instanceof MutableCompositeModification) {
            System.out.println(
                "Sending : " + data);
        } else {
            System.out.println(
                "Sending : " + ((KeyValue) data)
                    .getKey());

        }

        actorSelection.tell(appendEntries, null);

        actorSystem.actorOf(Props.create(ClientActor.class), "client");
    }

    public static AppendEntries modificationAppendEntries() {
        List<ReplicatedLogEntry> modification = new ArrayList<>();

        WriteModification writeModification =
                new WriteModification(TestModel.TEST_PATH, ImmutableNodes
                        .containerNode(TestModel.TEST_QNAME));

        MutableCompositeModification compositeModification =
                new MutableCompositeModification();

        compositeModification.addModification(writeModification);

        modification.add(0, new ReplicatedLogImplEntry(1, 1, compositeModification));

        return new AppendEntries(1, "member-1", 0, 100, modification, 1);
    }

    public static AppendEntries keyValueAppendEntries() {
        List<ReplicatedLogEntry> modification = new ArrayList<>();

        modification.add(0, new ReplicatedLogImplEntry(1, 1, new KeyValue("moiz", "test")));
        return new AppendEntries(1, "member-1", 0, 100, modification, 1);
    }
}
