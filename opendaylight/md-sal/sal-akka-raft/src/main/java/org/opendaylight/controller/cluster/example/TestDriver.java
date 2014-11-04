package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.raft.ConfigParams;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a test driver for testing akka-raft implementation
 * Its uses ExampleActors and threads to push content(key-vals) to these actors
 * Each ExampleActor can have one or more ClientActors. Each ClientActor spawns
 * a thread and starts push logs to the actor its assigned to.
 */
public class TestDriver {

    private static final ActorSystem actorSystem = ActorSystem.create();
    private static Map<String, String> allPeers = new HashMap<>();
    private static Map<String, ActorRef> clientActorRefs  = new HashMap<String, ActorRef>();
    private static Map<String, ActorRef> actorRefs = new HashMap<String, ActorRef>();
    private static LogGenerator logGenerator = new LogGenerator();
    private int nameCounter = 0;
    private static ConfigParams configParams = new ExampleConfigParamsImpl();

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
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
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

    public static ActorRef createExampleActor(String name) {
        return actorSystem.actorOf(ExampleActor.props(name, withoutPeer(name),
            Optional.of(configParams)), name);
    }

    public void createNodes(int num) {
        for (int i=0; i < num; i++)  {
            nameCounter = nameCounter + 1;
            allPeers.put("example-"+nameCounter, "akka://default/user/example-"+nameCounter);
        }

        for (String s : allPeers.keySet())  {
            ActorRef exampleActor = createExampleActor(s);
            actorRefs.put(s, exampleActor);
            System.out.println("Created node:"+s);

        }
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

