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

import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager.ShardManagerInfo;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager.ShardManagerInfoMBean;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;

import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ShardManager has the following jobs,
 * <ul>
 * <li> Create all the local shard replicas that belong on this cluster member
 * <li> Find the address of the local shard
 * <li> Find the primary replica for any given shard
 * <li> Monitor the cluster members and store their addresses
 * <ul>
 */
public class ShardManager extends AbstractUntypedActor {

    // Stores a mapping between a member name and the address of the member
    // Member names look like "member-1", "member-2" etc and are as specified
    // in configuration
    private final Map<String, Address> memberNameToAddress = new HashMap<>();

    // Stores a mapping between a shard name and it's corresponding information
    // Shard names look like inventory, topology etc and are as specified in
    // configuration
    private final Map<String, ShardInformation> localShards = new HashMap<>();

    // The type of a ShardManager reflects the type of the datastore itself
    // A data store could be of type config/operational
    private final String type;

    private final ClusterWrapper cluster;

    private final Configuration configuration;

    private ShardManagerInfoMBean mBean;

    private final ShardContext shardContext;

    /**
     * @param type defines the kind of data that goes into shards created by this shard manager. Examples of type would be
     *             configuration or operational
     */
    private ShardManager(String type, ClusterWrapper cluster, Configuration configuration,
            ShardContext shardContext) {

        this.type = Preconditions.checkNotNull(type, "type should not be null");
        this.cluster = Preconditions.checkNotNull(cluster, "cluster should not be null");
        this.configuration = Preconditions.checkNotNull(configuration, "configuration should not be null");
        this.shardContext = shardContext;

        // Subscribe this actor to cluster member events
        cluster.subscribeToMemberEvents(getSelf());

        // Create all the local Shards and make them a child of the ShardManager
        // TODO: This may need to be initiated when we first get the schema context
        createLocalShards();
    }

    public static Props props(final String type,
        final ClusterWrapper cluster,
        final Configuration configuration,
        final ShardContext shardContext) {

        Preconditions.checkNotNull(type, "type should not be null");
        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");

        return Props.create(new ShardManagerCreator(type, cluster, configuration, shardContext));
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if (message.getClass().equals(FindPrimary.SERIALIZABLE_CLASS)) {
            findPrimary(
                FindPrimary.fromSerializable(message));
        } else if(message instanceof FindLocalShard){
            findLocalShard((FindLocalShard) message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext(message);
        } else if (message instanceof ClusterEvent.MemberUp){
            memberUp((ClusterEvent.MemberUp) message);
        } else if(message instanceof ClusterEvent.MemberRemoved) {
            memberRemoved((ClusterEvent.MemberRemoved) message);
        } else if(message instanceof ClusterEvent.UnreachableMember) {
            ignoreMessage(message);
        } else{
            unknownMessage(message);
        }

    }

    private void findLocalShard(FindLocalShard message) {
        ShardInformation shardInformation =
            localShards.get(message.getShardName());

        if(shardInformation != null){
            getSender().tell(new LocalShardFound(shardInformation.getActor()), getSelf());
            return;
        }

        getSender().tell(new LocalShardNotFound(message.getShardName()),
            getSelf());
    }

    private void memberRemoved(ClusterEvent.MemberRemoved message) {
        memberNameToAddress.remove(message.member().roles().head());
    }

    private void memberUp(ClusterEvent.MemberUp message) {
        String memberName = message.member().roles().head();

        memberNameToAddress.put(memberName , message.member().address());

        for(ShardInformation info : localShards.values()){
            String shardName = info.getShardName();
            info.updatePeerAddress(getShardIdentifier(memberName, shardName),
                getShardActorPath(shardName, memberName));
        }
    }

    /**
     * Notifies all the local shards of a change in the schema context
     *
     * @param message
     */
    private void updateSchemaContext(Object message) {
        for(ShardInformation info : localShards.values()){
            info.getActor().tell(message,getSelf());
        }
    }

    private void findPrimary(FindPrimary message) {
        String shardName = message.getShardName();

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

        List<String> members =
            configuration.getMembersFromShardName(shardName);

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

    private String getShardActorPath(String shardName, String memberName) {
        Address address = memberNameToAddress.get(memberName);
        if(address != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(address.toString())
                .append("/user/")
                .append(ShardManagerIdentifier.builder().type(type).build().toString())
                .append("/")
                .append(getShardIdentifier(memberName, shardName));
            return builder.toString();
        }
        return null;
    }

    /**
     * Construct the name of the shard actor given the name of the member on
     * which the shard resides and the name of the shard
     *
     * @param memberName
     * @param shardName
     * @return
     */
    private ShardIdentifier getShardIdentifier(String memberName, String shardName){
        return ShardIdentifier.builder().memberName(memberName).shardName(shardName).type(type).build();
    }

    /**
     * Create shards that are local to the member on which the ShardManager
     * runs
     *
     */
    private void createLocalShards() {
        String memberName = this.cluster.getCurrentMemberName();
        List<String> memberShardNames =
            this.configuration.getMemberShardNames(memberName);

        List<String> localShardActorNames = new ArrayList<>();
        for(String shardName : memberShardNames){
            ShardIdentifier shardId = getShardIdentifier(memberName, shardName);
            Map<ShardIdentifier, String> peerAddresses = getPeerAddresses(shardName);
            ActorRef actor = getContext()
                .actorOf(Shard.props(shardId, peerAddresses, shardContext).
                    withMailbox(ActorContext.MAILBOX), shardId.toString());

            localShardActorNames.add(shardId.toString());
            localShards.put(shardName, new ShardInformation(shardName, actor, peerAddresses));
        }

        mBean = ShardManagerInfo
            .createShardManagerMBean("shard-manager-" + this.type, localShardActorNames);

    }

    /**
     * Given the name of the shard find the addresses of all it's peers
     *
     * @param shardName
     * @return
     */
    private Map<ShardIdentifier, String> getPeerAddresses(String shardName){

        Map<ShardIdentifier, String> peerAddresses = new HashMap<>();

        List<String> members =
            this.configuration.getMembersFromShardName(shardName);

        String currentMemberName = this.cluster.getCurrentMemberName();

        for(String memberName : members){
            if(!currentMemberName.equals(memberName)){
                ShardIdentifier shardId = getShardIdentifier(memberName,
                    shardName);
                String path =
                    getShardActorPath(shardName, currentMemberName);
                peerAddresses.put(shardId, path);
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
                    StringBuilder sb = new StringBuilder();
                    for(StackTraceElement element : t.getStackTrace()) {
                       sb.append("\n\tat ")
                         .append(element.toString());
                    }
                    LOG.warning("Supervisor Strategy of resume applied {}",sb.toString());
                    return SupervisorStrategy.resume();
                }
            }
        );

    }

    private class ShardInformation {
        private final String shardName;
        private final ActorRef actor;
        private final ActorPath actorPath;
        private final Map<ShardIdentifier, String> peerAddresses;

        private ShardInformation(String shardName, ActorRef actor,
            Map<ShardIdentifier, String> peerAddresses) {
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

        public void updatePeerAddress(ShardIdentifier peerId, String peerAddress){
            LOG.info("updatePeerAddress for peer {} with address {}", peerId,
                peerAddress);
            if(peerAddresses.containsKey(peerId)){
                peerAddresses.put(peerId, peerAddress);

                LOG.debug(
                    "Sending PeerAddressResolved for peer {} with address {} to {}",
                    peerId, peerAddress, actor.path());

                actor
                    .tell(new PeerAddressResolved(peerId, peerAddress),
                        getSelf());

            }
        }
    }

    private static class ShardManagerCreator implements Creator<ShardManager> {
        private static final long serialVersionUID = 1L;

        final String type;
        final ClusterWrapper cluster;
        final Configuration configuration;
        final ShardContext shardContext;

        ShardManagerCreator(String type, ClusterWrapper cluster,
                Configuration configuration, ShardContext shardContext) {
            this.type = type;
            this.cluster = cluster;
            this.configuration = configuration;
            this.shardContext = shardContext;
        }

        @Override
        public ShardManager create() throws Exception {
            return new ShardManager(type, cluster, configuration, shardContext);
        }
    }
}



