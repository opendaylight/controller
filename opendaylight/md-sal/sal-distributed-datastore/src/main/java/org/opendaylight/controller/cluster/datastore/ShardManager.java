package org.opendaylight.controller.cluster.datastore;

import akka.actor.Address;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ShardManager has the following jobs,
 *
 *  - Create all the local shard replicas that belong on this cluster member
 *  - Find the primary replica for any given shard
 *  - Engage in shard replica elections which decide which replica should be the primary
 *
 * Creation of Shard replicas
 * ==========================
 *  When the ShardManager is constructed it reads the cluster configuration to find out which shard replicas
 *  belong on this member. It finds out the name of the current cluster member from the Akka Clustering Service.
 *
 * Replica Elections
 * =================
 *  The Shard Manager uses multiple cues to initiate election.
 *      - When a member of the cluster dies
 *      - When a local shard replica dies
 *      - When a local shard replica comes alive
 */
public class ShardManager extends UntypedActor {

    // Stores a mapping between a shard name and the address of the current primary
    private final Map<String, Address> shardNameToPrimaryAddress = new HashMap<>();

    // Stores a mapping between a member name and the address of the member
    private final Map<String, Address> memberNameToAddress = new HashMap<>();

    // Stores a mapping between the shard name and all the members on which a replica of that shard are available
    private final Map<String, List<String>> shardNameToMembers = new HashMap<>();

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof FindPrimary ){
            FindPrimary msg = ((FindPrimary) message);
            getSender().tell(new PrimaryNotFound(msg.getShardName()), getSelf());
        }
    }
}
