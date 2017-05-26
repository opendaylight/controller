/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.raft.ConfigParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final ActorSystem actorSystem = ActorSystem.create();
    // Create three example actors
    private static Map<String, String> allPeers = new HashMap<>();

    static {
        allPeers.put("example-1", "akka://default/user/example-1");
        allPeers.put("example-2", "akka://default/user/example-2");
        allPeers.put("example-3", "akka://default/user/example-3");
    }

    public static void main(String[] args) throws Exception{
        ActorRef example1Actor =
            actorSystem.actorOf(ExampleActor.props("example-1",
                withoutPeer("example-1"), Optional.<ConfigParams>absent()), "example-1");

        ActorRef example2Actor =
            actorSystem.actorOf(ExampleActor.props("example-2",
                withoutPeer("example-2"), Optional.<ConfigParams>absent()), "example-2");

        ActorRef example3Actor =
            actorSystem.actorOf(ExampleActor.props("example-3",
                withoutPeer("example-3"), Optional.<ConfigParams>absent()), "example-3");


        List<ActorRef> examples = Arrays.asList(example1Actor, example2Actor, example3Actor);

        ActorRef clientActor = actorSystem.actorOf(ClientActor.props(example1Actor));
        BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Usage :");
        System.out.println("s <1-3> to start a peer");
        System.out.println("k <1-3> to kill a peer");

        while(true) {
            System.out.print("Enter command (0 to exit):");
            try {
                String s = br.readLine();
                String[] split = s.split(" ");
                if(split.length > 1) {
                    String command = split[0];
                    String actor = split[1];

                    if ("k".equals(command)) {
                        int i = Integer.parseInt(actor);
                        examples.get(i - 1)
                            .tell(PoisonPill.getInstance(), null);
                        continue;
                    } else if ("s".equals(command)) {
                        int i = Integer.parseInt(actor);
                        String actorName = "example-" + i;
                        examples.add(i - 1,
                            actorSystem.actorOf(ExampleActor.props(actorName,
                                withoutPeer(actorName), Optional.<ConfigParams>absent()),
                                actorName));
                        System.out.println("Created actor : " + actorName);
                        continue;
                    }
                }

                int i = Integer.parseInt(s);
                if(i == 0){
                    System.exit(0);
                }
                clientActor.tell(new KeyValue("key " + i, "value " + i), null);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid Format!");
            }
        }
    }

    private static Map<String, String> withoutPeer(String peerId) {
        Map<String, String> without = new HashMap<>(allPeers);
        without.remove(peerId);
        return without;
    }
}
