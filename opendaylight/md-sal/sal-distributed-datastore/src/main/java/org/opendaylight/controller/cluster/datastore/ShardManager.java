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
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
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

    private final Map<String, ShardInformation> localShards = new HashMap<>();


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
        String memberName = message.member().roles().head();

        memberNameToAddress.put(memberName , message.member().address());

        for(ShardInformation info : localShards.values()){
            String shardName = info.getShardName();
            info.updatePeerAddress(getShardActorName(memberName, shardName),
                getShardActorPath(shardName, memberName));
        }
    }

    private void updateSchemaContext(Object message) {
        for(ShardInformation info : localShards.values()){
            info.getActor().tell(message,getSelf());
        }
    }

    private void findPrimary(FindPrimary message) {
        String shardName = message.getShardName();

        List<String> members =
            configuration.getMembersFromShardName(shardName);

        // First see if the there is a local replica for the shard
        ShardInformation info = localShards.get(shardName);
        if(info != null) {
            ActorPath shardPath = info.getActorPath();
            if (shardPath != null) {
                getSender()
                    .tell(
                        new PrimaryFound(shardPath.toString()).toSerializable(),
                        getSelf());
                return;
            }
        }

        if(cluster.getCurrentMemberName() != null) {
            members.remove(cluster.getCurrentMemberName());
        }

        // There is no way for us to figure out the primary (for now) so assume
        // that one of the remote nodes is a primary
        for(String memberName : members) {
            Address address = memberNameToAddress.get(memberName);
            if(address != null){
                String path =
                    getShardActorPath(shardName, memberName);
                getSender().tell(new PrimaryFound(path).toSerializable(), getSelf());
                return;
            }
        }
        getSender().tell(new PrimaryNotFound(shardName).toSerializable(), getSelf());
    }

    private String


    getShardActorPath(String shardName, String memberName) {
        Address address = memberNameToAddress.get(memberName);
        if(address != null) {
            return address.toString() + "/user/shardmanager-" + this.type + "/"
                + getShardActorName(
                memberName, shardName);
        }
        return null;
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
            Map<String, String> peerAddresses = getPeerAddresses(shardName);
            ActorRef actor = getContext()
                .actorOf(Shard.props(shardActorName, peerAddresses),
                    shardActorName);
            localShards.put(shardName, new ShardInformation(shardName, actor, peerAddresses));
        }

    }

    private Map<String, String> getPeerAddresses(String shardName){

        Map<String, String> peerAddresses = new HashMap<>();

        List<String> members =
            this.configuration.getMembersFromShardName(shardName);

        String currentMemberName = this.cluster.getCurrentMemberName();

        for(String memberName : members){
            if(!currentMemberName.equals(memberName)){
                String shardActorName = getShardActorName(memberName, shardName);
                String path =
                    getShardActorPath(shardName, currentMemberName);
                peerAddresses.put(shardActorName, path);
            }
        }
        return peerAddresses;
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

    private class ShardInformation {
        private final String shardName;
        private final ActorRef actor;
        private final ActorPath actorPath;
        private final Map<String, String> peerAddresses;

        private ShardInformation(String shardName, ActorRef actor,
            Map<String, String> peerAddresses) {
            this.shardName = shardName;
            this.actor = actor;
            this.actorPath = actor.path();
            this.peerAddresses = peerAddresses;
        }

        public String getShardName() {
            return shardName;
        }

        public ActorRef getActor(){
            return actor;
        }

        public ActorPath getActorPath() {
            return actorPath;
        }

        public Map<String, String> getPeerAddresses() {
            return peerAddresses;
        }

        public void updatePeerAddress(String peerId, String peerAddress){
            LOG.info("updatePeerAddress for peer {} with address {}", peerId, peerAddress);
            if(peerAddresses.containsKey(peerId)){
                peerAddresses.put(peerId, peerAddress);

                LOG.info("Sending PeerAddressResolved for peer {} with address {} to {}", peerId, peerAddress, actor.path());

                actor
                    .tell(new PeerAddressResolved(peerId, peerAddress),
                        getSelf());

            }
        }
    }
}
