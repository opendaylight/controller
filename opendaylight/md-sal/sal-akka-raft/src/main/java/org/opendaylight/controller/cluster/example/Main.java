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

public class Main {
    private static final ActorSystem actorSystem = ActorSystem.create();
    public static void main(String[] args) throws Exception{
        ActorRef targetActor =
            actorSystem.actorOf(ExampleActor.props("shard-config-inventory-1"), "example");

        ActorRef clientActor = actorSystem.actorOf(ClientActor.props(targetActor));
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
}
