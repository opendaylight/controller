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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
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
    private static final Object GOSSIP_TICK = new Object() {
        @Override
        public String toString() {
            return "gossip tick";
        }
    };

    private final boolean autoStartGossipTicks;
    private final RemoteRpcProviderConfig config;

    /**
     * All known cluster members.
     */
    private final List<Address> clusterMembers = new ArrayList<>();

    /**
     * Cached ActorSelections for remote peers.
     */
    private final Map<Address, ActorSelection> peers = new HashMap<>();

    /**
     * ActorSystem's address for the current cluster node.
     */
    private Address selfAddress;

    private Cluster cluster;

    private Cancellable gossipTask;

    private BucketStoreAccess bucketStore;

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

        bucketStore = new BucketStoreAccess(getContext().parent(), getContext().dispatcher(), config.getAskDuration());

        if (provider instanceof ClusterActorRefProvider) {
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
                    GOSSIP_TICK,                                    //message
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

    @Override
    protected void handleReceive(final Object message) throws Exception {
        //Usually sent by self via gossip task defined above. But its not enforced.
        //These ticks can be sent by another actor as well which is esp. useful while testing
        if (GOSSIP_TICK.equals(message)) {
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
        LOG.debug("Received memberDown or Unreachable: {}", member);

        //if its self, then stop itself
        if (selfAddress.equals(member.address())) {
            getContext().stop(getSelf());
            return;
        }

        removePeer(member.address());
        LOG.debug("Removed member [{}], Active member list [{}]", member.address(), clusterMembers);
    }

    private void addPeer(final Address address) {
        if (!clusterMembers.contains(address)) {
            clusterMembers.add(address);
        }
        peers.computeIfAbsent(address, input -> getContext().system()
            .actorSelection(input.toString() + getSelf().path().toStringWithoutAddress()));
    }

    private void removePeer(final Address address) {
        clusterMembers.remove(address);
        peers.remove(address);
        bucketStore.removeRemoteBucket(address);
    }

    /**
     * Add member to the local copy of member list if it doesn't already.
     *
     * @param member the member to add
     */
    private void receiveMemberUpOrReachable(final Member member) {
        LOG.debug("Received memberUp or reachable: {}", member);

        //ignore up notification for self
        if (selfAddress.equals(member.address())) {
            return;
        }

        addPeer(member.address());
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
        final Address address;
        switch (clusterMembers.size()) {
            case 0:
                //no members to send gossip status to
                return;
            case 1:
                address = clusterMembers.get(0);
                break;
            default:
                final int randomIndex = ThreadLocalRandom.current().nextInt(0, clusterMembers.size());
                address = clusterMembers.get(randomIndex);
                break;
        }

        LOG.trace("Gossiping to [{}]", address);
        getLocalStatusAndSendTo(Verify.verifyNotNull(peers.get(address)));
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
        // Don't accept messages from non-members
        if (peers.containsKey(status.from())) {
            // FIXME: sender should be part of GossipStatus
            final ActorRef sender = getSender();
            bucketStore.getBucketVersions(versions ->  processRemoteStatus(sender, status, versions));
        }
    }

    private void processRemoteStatus(final ActorRef remote, final GossipStatus status,
            final Map<Address, Long> localVersions) {
        final Map<Address, Long> remoteVersions = status.versions();

        //diff between remote list and local
        final Set<Address> localIsOlder = new HashSet<>(remoteVersions.keySet());
        localIsOlder.removeAll(localVersions.keySet());

        //diff between local list and remote
        final Set<Address> localIsNewer = new HashSet<>(localVersions.keySet());
        localIsNewer.removeAll(remoteVersions.keySet());


        for (Entry<Address, Long> entry : remoteVersions.entrySet()) {
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
            remote.tell(new GossipStatus(selfAddress, localVersions), getSelf());
        }

        if (!localIsNewer.isEmpty()) {
            //send newer buckets to remote
            bucketStore.getBucketsByMembers(localIsNewer, buckets -> {
                LOG.trace("Buckets to send from {}: {}", selfAddress, buckets);
                remote.tell(new GossipEnvelope(selfAddress, remote.path().address(), buckets), getSelf());
            });
        }
    }

    /**
     * Sends the received buckets in the envelope to the parent Bucket store.
     *
     * @param envelope contains buckets from a remote gossiper
     */
    @VisibleForTesting
    void receiveGossip(final GossipEnvelope envelope) {
        //TODO: Add more validations
        if (!selfAddress.equals(envelope.to())) {
            LOG.trace("Ignoring message intended for someone else. From [{}] to [{}]", envelope.from(), envelope.to());
            return;
        }

        updateRemoteBuckets(envelope.buckets());
    }

    /**
     * Helper to send received buckets to bucket store.
     *
     * @param buckets map of Buckets to update
     */
    @VisibleForTesting
    void updateRemoteBuckets(final Map<Address, ? extends Bucket<?>> buckets) {
        // filter this so we only handle buckets for known peers
        bucketStore.updateRemoteBuckets(Maps.filterKeys(buckets, peers::containsKey));
    }

    /**
     * Gets bucket versions from bucket store and sends to the supplied address.
     *
     * @param remoteActorSystemAddress remote gossiper to send to
     */
    @VisibleForTesting
    void getLocalStatusAndSendTo(final ActorSelection remoteGossiper) {
        bucketStore.getBucketVersions(versions -> {
            LOG.trace("Sending bucket versions to [{}]", remoteGossiper);
            /*
             * XXX: we are leaking our reference here. That may be useful for establishing buckets monitoring,
             *      but can we identify which bucket is the local one?
             */
            remoteGossiper.tell(new GossipStatus(selfAddress, versions), getSelf());
        });
    }

    ///
    ///Getter Setters
    ///

    @VisibleForTesting
    void setClusterMembers(final Address... members) {
        clusterMembers.clear();
        peers.clear();

        for (Address addr : members) {
            addPeer(addr);
        }
    }
}
