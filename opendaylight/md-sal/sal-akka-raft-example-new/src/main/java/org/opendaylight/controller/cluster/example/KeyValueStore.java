package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.example.messages.KeyValueSaved;
import org.opendaylight.controller.cluster.example.messages.PrintRole;
import org.opendaylight.controller.cluster.example.messages.PrintState;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftClusterState;
import org.opendaylight.controller.cluster.raft.client.messages.*;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class KeyValueStore extends UntypedActor {

    Logger LOG = LoggerFactory.getLogger(KeyValueStore.class);

    private static final short RAFT_PAYLOAD_VERSION = (short)0;
    private Map<String, String> kvStore;
    private long persistenceIdentifier = 1;
    private ActorRef raftActor = null;

    private final String id;
    private final Map<String, String> peerAddresses;
    private final Optional<ConfigParams> configParams;
    private RaftClusterState clusterState = null;

    public KeyValueStore(String id, Map<String, String> peerAddresses,
                         Optional<ConfigParams> configParams) {
        this.id = id;
        this.peerAddresses = peerAddresses;
        this.configParams = configParams;
        kvStore = new ConcurrentHashMap<>();
    }

    public static Props props(String id, Map<String, String> peers,
                              Optional<ConfigParams> configParams) {
        if (!configParams.isPresent()) {
            configParams = Optional.of(new DefaultConfigParams());
        }
        return Props.create(KeyValueStore.class, id, peers, configParams);
    }

    @Override
    public void preStart() throws Exception {
        // Create Raft Actor here!
        raftActor = getContext().actorOf(
                RaftActor.props("raft-"+id, peerAddresses, configParams, RAFT_PAYLOAD_VERSION, true),
                "raft-"+id);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        // handle message from Raft
        if (message instanceof ApplyState) {
            ApplyState applyState = (ApplyState) message;
            applyState(applyState.getClientActor(), applyState.getIdentifier(), applyState.getData());
        } else if (message instanceof RaftClusterStateChanged) {
            RaftClusterStateChanged stateChanged = (RaftClusterStateChanged) message;
            clusterState = stateChanged.getClusterState();
        } else if (message instanceof ApplyLogRecoveryBatchRequest) {
            ApplyLogRecoveryBatchRequest request = (ApplyLogRecoveryBatchRequest) message;
            applyCurrentLogRecoveryBatch(request.getPayloads());
            getSender().tell(ApplyLogRecoveryBatchReply.INSTANCE, getSelf());
        } else if (message instanceof RecoveryComplete) {
            LOG.info("Recovery Complete");
        } else {
            // Then handle message from Client
            handleNonRaftCommand(message);
        }
    }

    private void handleNonRaftCommand(Object message) {
        if (clusterState == null) {
            LOG.error("Cluster State not available!");
        } else if (message instanceof KeyValue) {
            persistData(getSelf(), new PayloadIdentifier(persistenceIdentifier++), (Payload) message);
        } else if (message instanceof PrintState) {
            printState();
        } else if (message instanceof PrintRole) {
            printRole();
        } else {
            LOG.warn("Received unknown message: {}", message);
            unhandled(message);
        }
    }

    public void applyCurrentLogRecoveryBatch(List<Payload> payloads) {
        int length = payloads.size();
        LOG.info("id: {}, applyCurrentRecoveryBatch", id);
        for (int i = 0; i < length; i++) {
            KeyValue kv = (KeyValue) payloads.get(i);
            kvStore.put(kv.getKey(), kv.getValue());
        }
    }

    private void printState() {
        System.out.println("State for " + id +":");
        for (Map.Entry<String, String> entry: kvStore.entrySet()) {
            System.out.println("[Key: " + entry.getKey() + ", Value: " + entry.getValue() + "]");
        }
    }

    private void printRole() {
        System.out.println(id + ": isLeader: " + clusterState.isCurrentNodeLeader()
                + ", leaderId: " + clusterState.getLeaderId()
                + ", leader: " + clusterState.getLeader()
                + ", isLeaderActive: " + clusterState.isClusterLeaderActive());
    }

    // Method called to send PersistDataRequest to Raft
    private void persistData(ActorRef actorRef, Identifier identifier, Payload data) {
        if (clusterState.isCurrentNodeLeader() && clusterState.isClusterLeaderActive()) {
            LOG.debug("persistData to local raft");
            askRaftActorToPersistData(raftActor, actorRef, identifier, data);
        } else {
            LOG.debug("persistData to remote raft");
            askLeaderToPersistData(clusterState.getLeader(), actorRef, identifier, data);
        }
    }

    private void askRaftActorToPersistData(ActorRef raftActor,
                                           ActorRef actorRef, Identifier identifier, Payload data) {
        PersistDataRequest request = new PersistDataRequest(actorRef, identifier, data);
        final Future<Object> askFuture = Patterns.ask(raftActor, request, 5000);
        try {
            // TODO: Fix This! Reply is not a PersistDataReply object.
            PersistDataReply reply = (PersistDataReply) Await.result(askFuture,
                    Duration.create(5, TimeUnit.SECONDS));
            if (!reply.canPersist()) {
                clusterState = reply.getState();
                askLeaderToPersistData(clusterState.getLeader(), actorRef, identifier, data);
            }
        } catch (Exception e) {
            LOG.error("Cannot persist data due to: {}", e.getMessage());
        }
    }

    private void askLeaderToPersistData(ActorSelection leader,
                                        ActorRef actorRef, Identifier identifier, Payload data) {
        PersistDataRequest request = new PersistDataRequest(actorRef, identifier, data);
        Future<Object> askFuture = Patterns.ask(leader, request, 5000);
        try {
            // TODO: Fix This! Reply is not a PersistDataReply object.
            PersistDataReply reply = (PersistDataReply) Await.ready(askFuture,
                    Duration.create(5, TimeUnit.SECONDS));
            if (!reply.canPersist()) {
                askLeaderToPersistData(reply.getState().getLeader(), actorRef, identifier, data);
            }
        } catch (Exception e) {
            LOG.error("Cannot persist data due to: {}", e.getMessage());
        }
    }

    // Method called upon receiving ApplyState from Raft
    private void applyState(ActorRef clientActor, Identifier identifier, Object data) {
        if (data instanceof KeyValue) {
            KeyValue kv = (KeyValue)data;
            kvStore.put(kv.getKey(), kv.getValue());
            if (clientActor != null) {
                clientActor.tell(new KeyValueSaved(), getSelf());
            }
        }
    }

    // Used to identify the payload.
    private static final class PayloadIdentifier extends AbstractStringIdentifier<PayloadIdentifier> {
        private static final long serialVersionUID = 1L;

        PayloadIdentifier(final long identifier) {
            super(String.valueOf(identifier));
        }
    }

    private final static class DefaultConfigParams extends DefaultConfigParamsImpl {
        @Override
        public int getJournalRecoveryLogBatchSize() {
            return 10;
        }
    }
}
