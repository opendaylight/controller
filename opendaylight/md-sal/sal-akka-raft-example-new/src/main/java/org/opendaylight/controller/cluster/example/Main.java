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
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.example.messages.BulkKeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.raft.ConfigParams;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class Main {
    private static final ActorSystem actorSystem = ActorSystem.create();

    public static void main(String[] args) throws Exception {
        InputThread inputThread = new InputThread();
        inputThread.run();
    }

    private static Map<String, String> withoutPeer(Map<String, String> allPeers, String peerId) {
        Map<String, String> without = new HashMap<>(allPeers);
        without.remove(peerId);
        return without;
    }

    private static final class InputThread implements Runnable {

        private Map<String, ActorRef> kvStoreActors = new HashMap<>();
        private Map<String, ActorRef> clientActors = new HashMap<>();
        private Map<String, String> allNodes = null;

        @Override
        public void run() {
            Scanner lineScanner = new Scanner(System.in);
            while (lineScanner.hasNextLine()) {
                Scanner cmdScanner = new Scanner(lineScanner.nextLine());
                while (cmdScanner.hasNext()) {
                    switch (cmdScanner.next()) {
                        case "createNodes":
                            handleCreateNodes(cmdScanner);
                            break;
                        case "killNode":
                            handleKillNode(cmdScanner);
                            break;
                        case "reCreateNode":
                            handleReCreateNode(cmdScanner);
                            break;
                        case "add":
                            handleAdd(cmdScanner);
                            break;
                        case "bulkAdd":
                            handleBulkAdd(cmdScanner);
                            break;
                        case "printState":
                            handlePrintState(cmdScanner);
                            break;
                        case "printRole":
                            handlePrintRole(cmdScanner);
                            break;
                        default:
                            System.out.println("Invalid cmd!\n");
                            break;
                    }
                    cmdScanner.forEachRemaining(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            return;
                        }
                    });
                }
            }
        }

        // TODO: Improve. so many nested if's look stupid!

        private void handleCreateNodes(Scanner scanner) {
            if (scanner.hasNextInt()) {
                if (allNodes != null) {
                    System.err.println("Cannot Recreate Peers. Already Initiated");
                    return;
                }
                int numNodes = scanner.nextInt();
                if (numNodes < 3) {
                    System.err.println("At least specify 3 nodes!");
                    return;
                }
                allNodes = new HashMap<>(numNodes);
                for (int i = 1; i <= numNodes; i++) {
                    String member = "member-"+Integer.toString(i);
                    allNodes.put("raft-"+member, "akka://default/user/"+member+"/raft-"+member);
                }
                // Create KVStore and corresponding client Actors
                for (int i = 1; i <= numNodes; i++) {
                    String member = "member-"+Integer.toString(i);
                    String client = "client-"+Integer.toString(i);
                    ActorRef kvStoreActor = actorSystem.actorOf(KeyValueStore.props(
                            member,
                            withoutPeer(allNodes, member),
                            Optional.<ConfigParams>absent()), member);
                    ActorRef clientActor = actorSystem.actorOf(ClientActor.props(kvStoreActor), client);
                    kvStoreActors.put(member, kvStoreActor);
                    clientActors.put(client, clientActor);
                }
                return;
            }
            System.err.println("Invalid arguments to createNodes");
        }

        private void handleReCreateNode(Scanner scanner) {
            if (allNodes == null) {
                System.err.println("KV Store is not up yet. First use createNodes <num>");
                return;
            }
            if (scanner.hasNextInt()) {
                String nodeNum = scanner.next();
                String member = "member-"+nodeNum;
                if (!allNodes.containsKey("raft-"+member)) {
                    System.err.println("Adding new members is not allowed currently.");
                    return;
                }
                if (kvStoreActors.containsKey(member)) {
                    System.err.println("Node already present!");
                    return;
                }
                ActorRef kvStoreActor = actorSystem.actorOf(KeyValueStore.props(
                        member,
                        withoutPeer(allNodes, member),
                        Optional.<ConfigParams>absent()), member);
                ActorRef clientActor = actorSystem.actorOf(ClientActor.props(kvStoreActor),
                        "client-"+nodeNum);
                kvStoreActors.put(member, kvStoreActor);
                clientActors.put("client-"+nodeNum, clientActor);
                return;
            }
            System.err.println("Invalid arguments to reCreateNode");
        }

        private void handleKillNode(Scanner scanner) {
            if (scanner.hasNextInt()) {
                String nodeNum = scanner.next();
                String member = "member-"+nodeNum;
                String client = "client-"+nodeNum;
                ActorRef clientActor = clientActors.get(client);
                ActorRef kvStoreActor = kvStoreActors.get(member);
                if (kvStoreActor != null) {
                    actorSystem.stop(kvStoreActor);
                    kvStoreActors.remove(member);
                }
                if (clientActor != null) {
                    clientActors.remove(client);
                    actorSystem.stop(clientActor);
                }
                return;
            }
            System.err.println("Invalid arguments to kill nodes");
        }

        private void handleAdd(Scanner scanner) {
            if (scanner.hasNextInt()) {
                String member = scanner.next();
                if (clientActors.containsKey("client-"+member)) {
                    if (scanner.hasNextInt()) {
                        String key = scanner.next();
                        if (scanner.hasNextInt()) {
                            String value = scanner.next();
                            KeyValue kv = new KeyValue(key, value);
                            clientActors.get("client-"+member).tell(kv, null);
                            return;
                        }
                    }
                }
            }
            System.err.println("Invalid arguments to add!");
        }

        private void handleBulkAdd(Scanner scanner) {
            if (scanner.hasNextInt()) {
                String member = scanner.next();
                if (clientActors.containsKey("client-"+member)) {
                    if (scanner.hasNextInt()) {
                        int rangeStart = scanner.nextInt();
                        if (scanner.hasNextInt()) {
                            int rangeEnd = scanner.nextInt();
                            BulkKeyValue bkv = new BulkKeyValue(rangeStart, rangeEnd);
                            clientActors.get("client-"+member).tell(bkv, null);
                            return;
                        }
                    }
                }
            }
            System.err.println("Invalid arguments to bulkAdd!");
        }

        private void handlePrintState(Scanner scanner) {
            if (scanner.hasNextInt()) {
                String member = scanner.next();
                if (kvStoreActors.containsKey("member-"+member)) {
                    kvStoreActors.get("member-"+member).tell(new PrintState(), null);
                    return;
                }
            }
            System.err.println("Invalid arguments to printState!");
        }

        private void handlePrintRole(Scanner scanner) {
            if (scanner.hasNextInt()) {
                String member = scanner.next();
                if (kvStoreActors.containsKey("member-"+member)) {
                    kvStoreActors.get("member-"+member).tell(new PrintRole(), null);
                    return;
                }
            }
            System.err.println("Invalid arguments to printRole!");
        }
    }
}
