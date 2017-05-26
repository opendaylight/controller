/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.raft.ConfigParams;

/**
 * This is a test driver for testing akka-raft implementation
 * Its uses ExampleActors and threads to push content(key-vals) to these actors
 * Each ExampleActor can have one or more ClientActors. Each ClientActor spawns
 * a thread and starts push logs to the actor its assigned to.
 */
public class TestDriver {


    private static Map<String, String> allPeers = new HashMap<>();
    private static Map<String, ActorRef> clientActorRefs  = new HashMap<>();
    private static Map<String, ActorRef> actorRefs = new HashMap<>();
    private static LogGenerator logGenerator = new LogGenerator();
    private int nameCounter = 0;
    private static ConfigParams configParams = new ExampleConfigParamsImpl();

    private static ActorSystem actorSystem;
    private static ActorSystem listenerActorSystem;

    /**
     * Create nodes, add clients and start logging.
     * Commands
     *  bye
     *  createNodes:{num}
     *  stopNode:{nodeName}
     *  reinstateNode:{nodeName}
     *  addClients:{num}
     *  addClientsToNode:{nodeName, num}
     *  startLogging
     *  stopLogging
     *  startLoggingForClient:{nodeName}
     *  stopLoggingForClient:{nodeName}
     *  printNodes
     *  printState
     *
     *  Note: when run on IDE and on debug log level, the debug logs in
     *  AbstractUptypedActor and AbstractUptypedPersistentActor would need to be commented out.
     *  Also RaftActor handleCommand(), debug log which prints for every command other than AE/AER
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        actorSystem = ActorSystem.create("raft-test", ConfigFactory
            .load().getConfig("raft-test"));

        listenerActorSystem = ActorSystem.create("raft-test-listener", ConfigFactory
            .load().getConfig("raft-test-listener"));

        TestDriver td = new TestDriver();

        System.out.println("Enter command (type bye to exit):");


        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            String command = br.readLine();
            if (command.startsWith("bye")) {
                System.exit(0);

            } else if (command.startsWith("createNodes")) {
                String[] arr = command.split(":");
                int n = Integer.parseInt(arr[1]);
                td.createNodes(n);

            } else if (command.startsWith("addClients")) {
                String[] arr = command.split(":");
                int n = Integer.parseInt(arr[1]);
                td.addClients(n);

            } else if (command.startsWith("addClientsToNode")) {
                String[] arr = command.split(":");
                String nodeName = arr[1];
                int n = Integer.parseInt(arr[1]);
                td.addClientsToNode(nodeName, n);

            } else if (command.startsWith("stopNode")) {
                String[] arr = command.split(":");
                td.stopNode(arr[1]);

            } else if (command.startsWith("reinstateNode")) {
                String[] arr = command.split(":");
                td.reinstateNode(arr[1]);

            } else if (command.startsWith("startLogging")) {
                td.startAllLogging();

            } else if (command.startsWith("startLoggingForClient")) {
                String[] arr = command.split(":");
                td.startLoggingForClient(clientActorRefs.get(arr[1]));

            } else if (command.startsWith("stopLogging")) {
                td.stopAllLogging();

            } else if (command.startsWith("stopLoggingForClient")) {
                String[] arr = command.split(":");
                td.stopLoggingForClient(clientActorRefs.get(arr[1]));

            } else if (command.startsWith("printState")) {
                td.printState();
            } else if (command.startsWith("printNodes")) {
                td.printNodes();
            } else {
                System.out.println("Invalid command:" + command);
            }

        }
    }

    // create the listener using a separate actor system for each example actor
    private static void createClusterRoleChangeListener(List<String> memberIds) {
        System.out.println("memberIds="+memberIds);
        for (String memberId : memberIds) {
            ActorRef listenerActor = listenerActorSystem.actorOf(
                ExampleRoleChangeListener.getProps(memberId), memberId + "-role-change-listener");
            System.out.println("Role Change Listener created:" + listenerActor.path().toString());
        }
    }

    public static ActorRef createExampleActor(String name) {
        return actorSystem.actorOf(ExampleActor.props(name, withoutPeer(name),
            Optional.of(configParams)), name);
    }

    public void createNodes(int num) {
        for (int i=0; i < num; i++)  {
            nameCounter = nameCounter + 1;
            allPeers.put("example-"+nameCounter, "akka://raft-test/user/example-"+nameCounter);
        }

        for (String s : allPeers.keySet())  {
            ActorRef exampleActor = createExampleActor(s);
            actorRefs.put(s, exampleActor);
            System.out.println("Created node:"+s);

        }

        createClusterRoleChangeListener(Lists.newArrayList(allPeers.keySet()));
    }

    // add num clients to all nodes in the system
    public void addClients(int num) {
        for(Map.Entry<String,ActorRef> actorRefEntry : actorRefs.entrySet()) {
            for (int i=0; i < num; i++) {
                String clientName = "client-" + i + "-" + actorRefEntry.getKey();
                ActorRef clientActor = actorSystem.actorOf(
                    ClientActor.props(actorRefEntry.getValue()), clientName);
                clientActorRefs.put(clientName, clientActor);
                System.out.println("Created client-node:" + clientName);
            }
        }
    }

    // add num clients to a node
    public void addClientsToNode(String actorName, int num) {
        ActorRef actorRef = actorRefs.get(actorName);
        for (int i=0; i < num; i++) {
            String clientName = "client-" + i + "-" + actorName;
            clientActorRefs.put(clientName,
                actorSystem.actorOf(ClientActor.props(actorRef), clientName));
            System.out.println("Added client-node:" + clientName);
        }
    }

    public void stopNode(String actorName) {
        ActorRef actorRef = actorRefs.get(actorName);

        for (Map.Entry<String,ActorRef> entry : clientActorRefs.entrySet()) {
            if (entry.getKey().endsWith(actorName)) {
                actorSystem.stop(entry.getValue());
            }
        }

        actorSystem.stop(actorRef);
        actorRefs.remove(actorName);
        allPeers.remove(actorName);
    }

    public void reinstateNode(String actorName) {
        String address = "akka://default/user/"+actorName;
        allPeers.put(actorName, address);

        ActorRef exampleActor = createExampleActor(actorName);
        actorRefs.put(actorName, exampleActor);

        addClientsToNode(actorName, 1);
    }

    public void startAllLogging() {
        if(!clientActorRefs.isEmpty()) {
            for(Map.Entry<String,ActorRef> client : clientActorRefs.entrySet()) {
                logGenerator.startLoggingForClient(client.getValue());
                System.out.println("Started logging for client:"+client.getKey());
            }
        } else {
            System.out.println("There are no clients for any nodes. First create clients using commands- addClients:<num> or addClientsToNode:<nodename>:<num>");
        }

    }

    public void startLoggingForClient(ActorRef client) {
        logGenerator.startLoggingForClient(client);
    }

    public void stopAllLogging() {
        for(Map.Entry<String,ActorRef> client : clientActorRefs.entrySet()) {
            logGenerator.stopLoggingForClient(client.getValue());
        }
    }

    public void stopLoggingForClient(ActorRef client) {
        logGenerator.stopLoggingForClient(client);
    }

    public void printState() {
        for (ActorRef ref : actorRefs.values()) {
            ref.tell(new PrintState(), null);
        }
    }

    public void printNodes() {
        for (ActorRef ref : actorRefs.values()) {
            ref.tell(new PrintRole(), null);
        }
    }

    public ActorRef getLeader() {
        return null;
    }


    private static Map<String, String> withoutPeer(String peerId) {
        Map<String, String> without = new ConcurrentHashMap<>(allPeers);
        without.remove(peerId);

        return without;
    }
}

