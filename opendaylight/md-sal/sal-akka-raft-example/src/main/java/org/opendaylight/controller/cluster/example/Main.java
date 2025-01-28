/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.opendaylight.controller.cluster.example.messages.KeyValue;

public final class Main {
    private static final ActorSystem ACTOR_SYSTEM = ActorSystem.create();
    // Create three example actors
    private static Map<String, String> allPeers = new HashMap<>();

    static {
        allPeers.put("example-1", "pekko://default/user/example-1");
        allPeers.put("example-2", "pekko://default/user/example-2");
        allPeers.put("example-3", "pekko://default/user/example-3");
    }

    private Main() {
        // Hidden on purpose
    }

    @SuppressWarnings("checkstyle:RegexpSingleLineJava")
    public static void main(final String[] args) throws Exception {
        // use current directory for state
        final var stateDir = Path.of(".");

        ActorRef example1Actor =
            ACTOR_SYSTEM.actorOf(ExampleActor.props(stateDir, "example-1",
                withoutPeer("example-1"), Optional.empty()), "example-1");

        ActorRef example2Actor =
            ACTOR_SYSTEM.actorOf(ExampleActor.props(stateDir, "example-2",
                withoutPeer("example-2"), Optional.empty()), "example-2");

        ActorRef example3Actor =
            ACTOR_SYSTEM.actorOf(ExampleActor.props(stateDir, "example-3",
                withoutPeer("example-3"), Optional.empty()), "example-3");


        final var examples = Arrays.asList(example1Actor, example2Actor, example3Actor);

        final var clientActor = ACTOR_SYSTEM.actorOf(ClientActor.props(example1Actor));
        final var br = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));

        System.out.println("Usage :");
        System.out.println("s <1-3> to start a peer");
        System.out.println("k <1-3> to kill a peer");

        while (true) {
            System.out.print("Enter command (0 to exit):");
            try {
                String line = br.readLine();
                if (line == null) {
                    continue;
                }
                String[] split = line.split(" ");
                if (split.length > 1) {
                    String command = split[0];
                    String actor = split[1];

                    if ("k".equals(command)) {
                        int num = Integer.parseInt(actor);
                        examples.get(num - 1).tell(PoisonPill.getInstance(), null);
                        continue;
                    } else if ("s".equals(command)) {
                        int num = Integer.parseInt(actor);
                        String actorName = "example-" + num;
                        examples.add(num - 1,
                            ACTOR_SYSTEM.actorOf(ExampleActor.props(stateDir, actorName,
                                withoutPeer(actorName), Optional.empty()), actorName));
                        System.out.println("Created actor : " + actorName);
                        continue;
                    }
                }

                int num = Integer.parseInt(line);
                if (num == 0) {
                    System.exit(0);
                }
                clientActor.tell(new KeyValue("key " + num, "value " + num), null);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid Format!");
            }
        }
    }

    private static Map<String, String> withoutPeer(final String peerId) {
        Map<String, String> without = new HashMap<>(allPeers);
        without.remove(peerId);
        return without;
    }
}
