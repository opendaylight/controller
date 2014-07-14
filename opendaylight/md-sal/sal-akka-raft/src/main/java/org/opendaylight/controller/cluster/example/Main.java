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
import org.opendaylight.controller.cluster.example.messages.KeyValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
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
                withoutPeer("example-1")), "example-1");

        ActorRef example2Actor =
            actorSystem.actorOf(ExampleActor.props("example-2",
                withoutPeer("example-2")), "example-2");

        ActorRef example3Actor =
            actorSystem.actorOf(ExampleActor.props("example-3",
                withoutPeer("example-3")), "example-3");

        ActorRef clientActor = actorSystem.actorOf(ClientActor.props(example1Actor));
        BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));

        while(true) {
            System.out.print("Enter Integer (0 to exit):");
            try {
                int i = Integer.parseInt(br.readLine());
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
