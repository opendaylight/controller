/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.cluster.ClusterEvent;
import akka.japi.Creator;
import akka.japi.Function;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ShardManager has the following jobs,
 * <p>
 * <li> Create all the local shard replicas that belong on this cluster member
 * <li> Find the primary replica for any given shard
 * <li> Engage in shard replica elections which decide which replica should be the primary
 * </p>
 * <p/>
 * <h3>>Creation of Shard replicas</h3
 * <p>
 * When the ShardManager is constructed it reads the cluster configuration to find out which shard replicas
 * belong on this member. It finds out the name of the current cluster member from the Akka Clustering Service.
 * </p>
 * <p/>
 * <h3> Replica Elections </h3>
 * <p/>
 * <p>
 * The Shard Manager uses multiple cues to initiate election.
 * <li> When a member of the cluster dies
 * <li> When a local shard replica dies
 * <li> When a local shard replica comes alive
 * </p>
 */
public class ShardManager extends AbstractUntypedActor {

    // Stores a mapping between a member name and the address of the member
    private final Map<String, Address> memberNameToAddress = new HashMap<>();

    private final Map<String, ActorPath> localShards = new HashMap<>();


    private final String type;

    private final ClusterWrapper cluster;

    private final Configuration configuration;

    /**
     * @param type defines the kind of data that goes into shards created by this shard manager. Examples of type would be
     *             configuration or operational
     */
    private ShardManager(String type, ClusterWrapper cluster, Configuration configuration) {

        this.type = Preconditions.checkNotNull(type, "type should not be null");
        this.cluster = Preconditions.checkNotNull(cluster, "cluster should not be null");
        this.configuration = Preconditions.checkNotNull(configuration, "configuration should not be null");

        // Subscribe this actor to cluster member events
        cluster.subscribeToMemberEvents(getSelf());

        // Create all the local Shards and make them a child of the ShardManager
        // TODO: This may need to be initiated when we first get the schema context
        createLocalShards();
    }

    public static Props props(final String type,
        final ClusterWrapper cluster,
        final Configuration configuration) {
        return Props.create(new Creator<ShardManager>() {

            @Override
            public ShardManager create() throws Exception {
                return new ShardManager(type, cluster, configuration);
            }
        });
    }


    @Override
    public void handleReceive(Object message) throws Exception {
        if (message.getClass().equals(FindPrimary.SERIALIZABLE_CLASS)) {
            findPrimary(
                FindPrimary.fromSerializable(message));

        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext(message);
        } else if (message instanceof ClusterEvent.MemberUp){
            memberUp((ClusterEvent.MemberUp) message);
        } else if(message instanceof ClusterEvent.MemberRemoved) {
            memberRemoved((ClusterEvent.MemberRemoved) message);
        } else if(message instanceof ClusterEvent.UnreachableMember) {
            ignoreMessage(message);
        } else{
          throw new Exception ("Not recognized message received, message="+message);
        }

    }

    private void ignoreMessage(Object message){
        LOG.debug("Unhandled message : " + message);
    }

    private void memberRemoved(ClusterEvent.MemberRemoved message) {
        memberNameToAddress.remove(message.member().roles().head());
    }

    private void memberUp(ClusterEvent.MemberUp message) {
        memberNameToAddress.put(message.member().roles().head(), message.member().address());
    }

    private void updateSchemaContext(Object message) {
        for(ActorPath path : localShards.values()){
            getContext().system().actorSelection(path)
                .forward(message,
                    getContext());
        }
    }

    private void findPrimary(FindPrimary message) {
        String shardName = message.getShardName();

        List<String> members =
            configuration.getMembersFromShardName(shardName);

        for(String memberName : members) {
            if (memberName.equals(cluster.getCurrentMemberName())) {
                // This is a local shard
                ActorPath shardPath = localShards.get(shardName);
                if (shardPath == null) {
                    getSender()
                        .tell(new PrimaryNotFound(shardName).toSerializable(), getSelf());
                    return;
                }
                getSender().tell(new PrimaryFound(shardPath.toString()).toSerializable(),
                    getSelf());
                return;
            } else {
                Address address = memberNameToAddress.get(memberName);
                if(address != null){
                    String path =
                        address.toString() + "/user/shardmanager-" + this.type + "/" + getShardActorName(
                            memberName, shardName);
                    getSender().tell(new PrimaryFound(path).toSerializable(), getSelf());
                    return;
                }


            }
        }

        getSender().tell(new PrimaryNotFound(shardName).toSerializable(), getSelf());
    }

    private String getShardActorName(String memberName, String shardName){
        return memberName + "-shard-" + shardName + "-" + this.type;
    }

    // Create the shards that are local to this member
    private void createLocalShards() {
        String memberName = this.cluster.getCurrentMemberName();
        List<String> memberShardNames =
            this.configuration.getMemberShardNames(memberName);

        for(String shardName : memberShardNames){
            String shardActorName = getShardActorName(memberName, shardName);
            ActorRef actor = getContext()
                .actorOf(Shard.props(shardActorName), shardActorName);
            ActorPath path = actor.path();
            localShards.put(shardName, path);
        }

    }


    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.create("1 minute"),
            new Function<Throwable, SupervisorStrategy.Directive>() {
                @Override
                public SupervisorStrategy.Directive apply(Throwable t) {
                    return SupervisorStrategy.resume();
                }
            }
        );

    }
}
