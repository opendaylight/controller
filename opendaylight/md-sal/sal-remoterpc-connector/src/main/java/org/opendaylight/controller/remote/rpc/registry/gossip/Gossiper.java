/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterActorRefProvider;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersions;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketVersionsReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembers;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.GetBucketsByMembersReply;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.RemoveRemoteBucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.BucketStoreMessages.UpdateRemoteBuckets;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus;
import org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipTick;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Gossiper that syncs bucket store across nodes in the cluster.
 *
 * <p>
 * It keeps a local scheduler that periodically sends Gossip ticks to
 * itself to send bucket store's bucket versions to a randomly selected remote
 * gossiper.
 *
 * <p>
 * When bucket versions are received from a remote gossiper, it is compared
 * with bucket store's bucket versions. Which ever buckets are newer
 * locally, are sent to remote gossiper. If any bucket is older in bucket store,
 * a gossip status is sent to remote gossiper so that it can send the newer buckets.
 *
 * <p>
 * When a bucket is received from a remote gossiper, its sent to the bucket store
 * for update.
 */
public class Gossiper extends AbstractUntypedActorWithMetering {
    private final boolean autoStartGossipTicks;
    private final RemoteRpcProviderConfig config;

    /**
     * All known cluster members.
     */
    private final List<Address> clusterMembers = new ArrayList<>();

    /**
     * ActorSystem's address for the current cluster node.
     */
    private Address selfAddress;

    private Cluster cluster;

    private Cancellable gossipTask;

    Gossiper(final RemoteRpcProviderConfig config, final Boolean autoStartGossipTicks) {
        this.config = Preconditions.checkNotNull(config);
        this.autoStartGossipTicks = autoStartGossipTicks.booleanValue();
    }

    Gossiper(final RemoteRpcProviderConfig config) {
        this(config, Boolean.TRUE);
    }

    public static Props props(final RemoteRpcProviderConfig config) {
        return Props.create(Gossiper.class, config);
    }

    static Props testProps(final RemoteRpcProviderConfig config) {
        return Props.create(Gossiper.class, config, Boolean.FALSE);
    }

    @Override
    public void preStart() {
        ActorRefProvider provider = getContext().provider();
        selfAddress = provider.getDefaultAddress();

        if (provider instanceof ClusterActorRefProvider ) {
            cluster = Cluster.get(getContext().system());
            cluster.subscribe(getSelf(),
                    ClusterEvent.initialStateAsEvents(),
                    ClusterEvent.MemberEvent.class,
                    ClusterEvent.ReachableMember.class,
                    ClusterEvent.UnreachableMember.class);
        }

        if (autoStartGossipTicks) {
            gossipTask = getContext().system().scheduler().schedule(
                    new FiniteDuration(1, TimeUnit.SECONDS),        //initial delay
                    config.getGossipTickInterval(),                 //interval
                    getSelf(),                                      //target
                    new Messages.GossiperMessages.GossipTick(),     //message
                    getContext().dispatcher(),                      //execution context
                    getSelf()                                       //sender
            );
        }
    }

    @Override
    public void postStop() {
        if (cluster != null) {
            cluster.unsubscribe(getSelf());
        }
        if (gossipTask != null) {
            gossipTask.cancel();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void handleReceive(final Object message) throws Exception {
        //Usually sent by self via gossip task defined above. But its not enforced.
        //These ticks can be sent by another actor as well which is esp. useful while testing
        if (message instanceof GossipTick) {
            receiveGossipTick();
        } else if (message instanceof GossipStatus) {
            // Message from remote gossiper with its bucket versions
            receiveGossipStatus((GossipStatus) message);
        } else if (message instanceof GossipEnvelope) {
            // Message from remote gossiper with buckets. This is usually in response to GossipStatus
            // message. The contained buckets are newer as determined by the remote gossiper by
            // comparing the GossipStatus message with its local versions.
            receiveGossip((GossipEnvelope) message);
        } else if (message instanceof ClusterEvent.MemberUp) {
            receiveMemberUpOrReachable(((ClusterEvent.MemberUp) message).member());

        } else if (message instanceof ClusterEvent.ReachableMember) {
            receiveMemberUpOrReachable(((ClusterEvent.ReachableMember) message).member());

        } else if (message instanceof ClusterEvent.MemberRemoved) {
            receiveMemberRemoveOrUnreachable(((ClusterEvent.MemberRemoved) message).member());

        } else if (message instanceof ClusterEvent.UnreachableMember) {
            receiveMemberRemoveOrUnreachable(((ClusterEvent.UnreachableMember) message).member());

        } else {
            unhandled(message);
        }
    }

    /**
     * Remove member from local copy of member list. If member down is self, then stop the actor
     *
     * @param member who went down
     */
    private void receiveMemberRemoveOrUnreachable(final Member member) {
        //if its self, then stop itself
        if (selfAddress.equals(member.address())) {
            getContext().stop(getSelf());
            return;
        }

        clusterMembers.remove(member.address());
        LOG.debug("Removed member [{}], Active member list [{}]", member.address(), clusterMembers);

        getContext().parent().tell(new RemoveRemoteBucket(member.address()), ActorRef.noSender());
    }

    /**
     * Add member to the local copy of member list if it doesn't already.
     *
     * @param member the member to add
     */
    private void receiveMemberUpOrReachable(final Member member) {

        if (selfAddress.equals(member.address())) {
            //ignore up notification for self
            return;
        }

        if (!clusterMembers.contains(member.address())) {
            clusterMembers.add(member.address());
        }

        LOG.debug("Added member [{}], Active member list [{}]", member.address(), clusterMembers);
    }

    /**
     * Sends Gossip status to other members in the cluster.
     * <br>
     * 1. If there are no member, ignore the tick. <br>
     * 2. If there's only 1 member, send gossip status (bucket versions) to it. <br>
     * 3. If there are more than one member, randomly pick one and send gossip status (bucket versions) to it.
     */
    @VisibleForTesting
    void receiveGossipTick() {
        final Address remoteMemberToGossipTo;
        switch (clusterMembers.size()) {
            case 0:
                //no members to send gossip status to
                return;
            case 1:
                remoteMemberToGossipTo = clusterMembers.get(0);
                break;
            default:
                final int randomIndex = ThreadLocalRandom.current().nextInt(0, clusterMembers.size());
                remoteMemberToGossipTo = clusterMembers.get(randomIndex);
                break;
        }

        LOG.trace("Gossiping to [{}]", remoteMemberToGossipTo);
        getLocalStatusAndSendTo(remoteMemberToGossipTo);
    }

    /**
     * Process gossip status received from a remote gossiper. Remote versions are compared with
     * the local copy.
     * <p/>
     * For each bucket
     * <ul>
     *  <li>If local copy is newer, the newer buckets are sent in GossipEnvelope to remote</li>
     *  <li>If local is older, GossipStatus is sent to remote so that it can reply with GossipEnvelope</li>
     *  <li>If both are same, noop</li>
     * </ul>
     *
     * @param status bucket versions from a remote member
     */
    @VisibleForTesting
    void receiveGossipStatus(final GossipStatus status) {
        //Don't accept messages from non-members
        if (!clusterMembers.contains(status.from())) {
            return;
        }

        final ActorRef sender = getSender();
        Future<Object> futureReply =
                Patterns.ask(getContext().parent(), new GetBucketVersions(), config.getAskDuration());

        futureReply.map(getMapperToProcessRemoteStatus(sender, status), getContext().dispatcher());
    }

    /**
     * Sends the received buckets in the envelope to the parent Bucket store.
     *
     * @param envelope contains buckets from a remote gossiper
     */
    @VisibleForTesting
    <T extends Copier<T>> void receiveGossip(final GossipEnvelope<T> envelope) {
        //TODO: Add more validations
        if (!selfAddress.equals(envelope.to())) {
            LOG.trace("Ignoring message intended for someone else. From [{}] to [{}]", envelope.from(), envelope.to());
            return;
        }

        updateRemoteBuckets(envelope.getBuckets());
    }

    /**
     * Helper to send received buckets to bucket store.
     *
     * @param buckets map of Buckets to update
     */
    @VisibleForTesting
    <T extends Copier<T>> void updateRemoteBuckets(final Map<Address, Bucket<T>> buckets) {
        getContext().parent().tell(new UpdateRemoteBuckets<>(buckets), getSelf());
    }

    /**
     * Gets the buckets from bucket store for the given node addresses and sends them to remote gossiper.
     *
     * @param remote     remote node to send Buckets to
     * @param addresses  node addresses whose buckets needs to be sent
     */
    void sendGossipTo(final ActorRef remote, final Set<Address> addresses) {

        Future<Object> futureReply =
                Patterns.ask(getContext().parent(), new GetBucketsByMembers(addresses), config.getAskDuration());
        futureReply.map(getMapperToSendGossip(remote), getContext().dispatcher());
    }

    /**
     * Gets bucket versions from bucket store and sends to the supplied address.
     *
     * @param remoteActorSystemAddress remote gossiper to send to
     */
    @VisibleForTesting
    void getLocalStatusAndSendTo(final Address remoteActorSystemAddress) {

        //Get local status from bucket store and send to remote
        Future<Object> futureReply =
                Patterns.ask(getContext().parent(), new GetBucketVersions(), config.getAskDuration());

        //Find gossiper on remote system
        ActorSelection remoteRef = getContext().system().actorSelection(
                remoteActorSystemAddress.toString() + getSelf().path().toStringWithoutAddress());

        LOG.trace("Sending bucket versions to [{}]", remoteRef);

        futureReply.map(getMapperToSendLocalStatus(remoteRef), getContext().dispatcher());
    }

    /**
     * Helper to send bucket versions received from local store.
     *
     * @param remote        remote gossiper to send versions to
     * @param localVersions bucket versions received from local store
     */
    void sendGossipStatusTo(final ActorRef remote, final Map<Address, Long> localVersions) {

        GossipStatus status = new GossipStatus(selfAddress, localVersions);
        remote.tell(status, getSelf());
    }

    void sendGossipStatusTo(final ActorSelection remote, final Map<Address, Long> localVersions) {

        GossipStatus status = new GossipStatus(selfAddress, localVersions);
        remote.tell(status, getSelf());
    }

    ///
    /// Private factories to create mappers
    ///

    private Mapper<Object, Void> getMapperToSendLocalStatus(final ActorSelection remote) {

        return new Mapper<Object, Void>() {
            @Override
            public Void apply(final Object replyMessage) {
                if (replyMessage instanceof GetBucketVersionsReply) {
                    GetBucketVersionsReply reply = (GetBucketVersionsReply) replyMessage;
                    Map<Address, Long> localVersions = reply.getVersions();

                    sendGossipStatusTo(remote, localVersions);

                }
                return null;
            }
        };
    }

    /**
     * Process bucket versions received from
     * {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore}.
     * Then this method compares remote bucket versions with local bucket versions.
     * <ul>
     *     <li>The buckets that are newer locally, send
     *     {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope}
     *     to remote
     *     <li>The buckets that are older locally, send
     *     {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus}
     *     to remote so that remote sends GossipEnvelop.
     * </ul>
     *
     * @param sender the remote member
     * @param status bucket versions from a remote member
     * @return a {@link akka.dispatch.Mapper} that gets evaluated in future
     *
     */
    private Mapper<Object, Void> getMapperToProcessRemoteStatus(final ActorRef sender, final GossipStatus status) {

        final Map<Address, Long> remoteVersions = status.getVersions();

        return new Mapper<Object, Void>() {
            @Override
            public Void apply(final Object replyMessage) {
                if (replyMessage instanceof GetBucketVersionsReply) {
                    GetBucketVersionsReply reply = (GetBucketVersionsReply) replyMessage;
                    Map<Address, Long> localVersions = reply.getVersions();

                    //diff between remote list and local
                    Set<Address> localIsOlder = new HashSet<>();
                    localIsOlder.addAll(remoteVersions.keySet());
                    localIsOlder.removeAll(localVersions.keySet());

                    //diff between local list and remote
                    Set<Address> localIsNewer = new HashSet<>();
                    localIsNewer.addAll(localVersions.keySet());
                    localIsNewer.removeAll(remoteVersions.keySet());


                    for (Map.Entry<Address, Long> entry : remoteVersions.entrySet()) {
                        Address address = entry.getKey();
                        Long remoteVersion = entry.getValue();
                        Long localVersion = localVersions.get(address);
                        if (localVersion == null || remoteVersion == null) {
                            //this condition is taken care of by above diffs
                            continue;
                        }

                        if (localVersion < remoteVersion) {
                            localIsOlder.add(address);
                        } else if (localVersion > remoteVersion) {
                            localIsNewer.add(address);
                        }
                    }

                    if (!localIsOlder.isEmpty()) {
                        sendGossipStatusTo(sender, localVersions);
                    }

                    if (!localIsNewer.isEmpty()) {
                        //send newer buckets to remote
                        sendGossipTo(sender, localIsNewer);
                    }
                }
                return null;
            }
        };
    }

    /**
     * Processes the message from {@link org.opendaylight.controller.remote.rpc.registry.gossip.BucketStore}
     * that contains {@link org.opendaylight.controller.remote.rpc.registry.gossip.Bucket}.
     * These buckets are sent to a remote member encapsulated in
     * {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipEnvelope}
     *
     * @param sender the remote member that sent
     *           {@link org.opendaylight.controller.remote.rpc.registry.gossip.Messages.GossiperMessages.GossipStatus}
     *           in reply to which bucket is being sent back
     * @return a {@link akka.dispatch.Mapper} that gets evaluated in future
     *
     */
    private Mapper<Object, Void> getMapperToSendGossip(final ActorRef sender) {

        return new Mapper<Object, Void>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public Void apply(final Object msg) {
                if (msg instanceof GetBucketsByMembersReply) {
                    Map<Address, Bucket<?>> buckets = ((GetBucketsByMembersReply) msg).getBuckets();
                    LOG.trace("Buckets to send from {}: {}", selfAddress, buckets);
                    GossipEnvelope envelope = new GossipEnvelope(selfAddress, sender.path().address(), buckets);
                    sender.tell(envelope, getSelf());
                }
                return null;
            }
        };
    }

    ///
    ///Getter Setters
    ///

    @VisibleForTesting
    void setClusterMembers(final Address... members) {
        clusterMembers.clear();
        clusterMembers.addAll(Arrays.asList(members));
    }
}
