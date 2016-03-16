/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_TYPES_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_TYPE_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_TYPE_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidateMapEntry;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidateNodeKey;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.createEntity;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.pattern.Patterns;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateAdded;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateRemoved;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.SelectOwner;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategy;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Special Shard for EntityOwnership.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipShard extends Shard {
    private final String localMemberName;
    private final EntityOwnershipShardCommitCoordinator commitCoordinator;
    private final EntityOwnershipListenerSupport listenerSupport;
    private final Set<String> downPeerMemberNames = new HashSet<>();
    private final Map<String, String> peerIdToMemberNames = new HashMap<>();
    private final EntityOwnerSelectionStrategyConfig strategyConfig;
    private final Map<YangInstanceIdentifier, Cancellable> entityToScheduledOwnershipTask = new HashMap<>();
    private final EntityOwnershipStatistics entityOwnershipStatistics;

    private static DatastoreContext noPersistenceDatastoreContext(DatastoreContext datastoreContext) {
        return DatastoreContext.newBuilderFrom(datastoreContext).persistent(false).build();
    }

    protected EntityOwnershipShard(Builder builder) {
        super(builder);
        this.localMemberName = builder.localMemberName;
        this.commitCoordinator = new EntityOwnershipShardCommitCoordinator(builder.localMemberName, LOG);
        this.listenerSupport = new EntityOwnershipListenerSupport(getContext(), persistenceId());
        this.strategyConfig = builder.ownerSelectionStrategyConfig;
        this.entityOwnershipStatistics = new EntityOwnershipStatistics();
        this.entityOwnershipStatistics.init(getDataStore());

        for(String peerId: getRaftActorContext().getPeerIds()) {
            ShardIdentifier shardId = ShardIdentifier.builder().fromShardIdString(peerId).build();
            peerIdToMemberNames.put(peerId, shardId.getMemberName());
        }
    }

    @Override
    protected void onDatastoreContext(DatastoreContext context) {
        super.onDatastoreContext(noPersistenceDatastoreContext(context));
    }

    @Override
    protected void onRecoveryComplete() {
        super.onRecoveryComplete();

        new CandidateListChangeListener(getSelf(), persistenceId()).init(getDataStore());
        new EntityOwnerChangeListener(localMemberName, listenerSupport).init(getDataStore());
    }

    @Override
    public void handleCommand(final Object message) {
        if(message instanceof RegisterCandidateLocal) {
            onRegisterCandidateLocal((RegisterCandidateLocal) message);
        } else if(message instanceof UnregisterCandidateLocal) {
            onUnregisterCandidateLocal((UnregisterCandidateLocal)message);
        } else if(message instanceof CandidateAdded){
            onCandidateAdded((CandidateAdded) message);
        } else if(message instanceof CandidateRemoved){
            onCandidateRemoved((CandidateRemoved) message);
        } else if(message instanceof PeerDown) {
            onPeerDown((PeerDown) message);
        } else if(message instanceof PeerUp) {
            onPeerUp((PeerUp) message);
        } else if(message instanceof RegisterListenerLocal) {
            onRegisterListenerLocal((RegisterListenerLocal)message);
        } else if(message instanceof UnregisterListenerLocal) {
            onUnregisterListenerLocal((UnregisterListenerLocal) message);
        } else if(message instanceof SelectOwner) {
            onSelectOwner((SelectOwner) message);
        } else if(!commitCoordinator.handleMessage(message, this)) {
            super.handleCommand(message);
        }
    }

    private void onSelectOwner(SelectOwner selectOwner) {
        LOG.debug("{}: onSelectOwner: {}", persistenceId(), selectOwner);

        String currentOwner = getCurrentOwner(selectOwner.getEntityPath());
        if(Strings.isNullOrEmpty(currentOwner)) {
            String entityType = EntityOwnersModel.entityTypeFromEntityPath(selectOwner.getEntityPath());
            writeNewOwner(selectOwner.getEntityPath(), newOwner(currentOwner, selectOwner.getAllCandidates(),
                    entityOwnershipStatistics.byEntityType(entityType),
                    selectOwner.getOwnerSelectionStrategy()));

            Cancellable cancellable = entityToScheduledOwnershipTask.get(selectOwner.getEntityPath());
            if(cancellable != null){
                if(!cancellable.isCancelled()){
                    cancellable.cancel();
                }
                entityToScheduledOwnershipTask.remove(selectOwner.getEntityPath());
            }
        }
    }

    private void onRegisterCandidateLocal(RegisterCandidateLocal registerCandidate) {
        LOG.debug("{}: onRegisterCandidateLocal: {}", persistenceId(), registerCandidate);

        listenerSupport.setHasCandidateForEntity(registerCandidate.getEntity());

        NormalizedNode<?, ?> entityOwners = entityOwnersWithCandidate(registerCandidate.getEntity().getType(),
                registerCandidate.getEntity().getId(), localMemberName);
        commitCoordinator.commitModification(new MergeModification(ENTITY_OWNERS_PATH, entityOwners), this);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    private void onUnregisterCandidateLocal(UnregisterCandidateLocal unregisterCandidate) {
        LOG.debug("{}: onUnregisterCandidateLocal: {}", persistenceId(), unregisterCandidate);

        Entity entity = unregisterCandidate.getEntity();
        listenerSupport.unsetHasCandidateForEntity(entity);

        YangInstanceIdentifier candidatePath = candidatePath(entity.getType(), entity.getId(), localMemberName);
        commitCoordinator.commitModification(new DeleteModification(candidatePath), this);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    private void onRegisterListenerLocal(final RegisterListenerLocal registerListener) {
        LOG.debug("{}: onRegisterListenerLocal: {}", persistenceId(), registerListener);

        listenerSupport.addEntityOwnershipListener(registerListener.getEntityType(), registerListener.getListener());

        getSender().tell(SuccessReply.INSTANCE, getSelf());

        searchForEntities(new EntityWalker() {
            @Override
            public void onEntity(MapEntryNode entityTypeNode, MapEntryNode entityNode) {
                Optional<DataContainerChild<?, ?>> possibleType = entityTypeNode.getChild(ENTITY_TYPE_NODE_ID);
                String entityType = possibleType.isPresent() ? possibleType.get().getValue().toString() : null;
                if (registerListener.getEntityType().equals(entityType)) {
                    final boolean hasOwner;
                    final boolean isOwner;

                    Optional<DataContainerChild<?, ?>> possibleOwner = entityNode.getChild(ENTITY_OWNER_NODE_ID);
                    if (possibleOwner.isPresent()) {
                        isOwner = localMemberName.equals(possibleOwner.get().getValue().toString());
                        hasOwner = true;
                    } else {
                        isOwner = false;
                        hasOwner = false;
                    }

                    Entity entity = new Entity(entityType,
                        (YangInstanceIdentifier) entityNode.getChild(ENTITY_ID_NODE_ID).get().getValue());

                    listenerSupport.notifyEntityOwnershipListener(entity, false, isOwner, hasOwner,
                        registerListener.getListener());
                }
            }
        });
    }

    private void onUnregisterListenerLocal(UnregisterListenerLocal unregisterListener) {
        LOG.debug("{}: onUnregisterListenerLocal: {}", persistenceId(), unregisterListener);

        listenerSupport.removeEntityOwnershipListener(unregisterListener.getEntityType(), unregisterListener.getListener());

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    void tryCommitModifications(final BatchedModifications modifications) {
        if(isLeader()) {
            LOG.debug("{}: Committing BatchedModifications {} locally", persistenceId(), modifications.getTransactionID());

            // Note that it's possible the commit won't get consensus and will timeout and not be applied
            // to the state. However we don't need to retry it in that case b/c it will be committed to
            // the journal first and, once a majority of followers come back on line and it is replicated,
            // it will be applied at that point.
            handleBatchedModificationsLocal(modifications, self());
        } else {
            final ActorSelection leader = getLeader();
            if (leader != null) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("{}: Sending BatchedModifications {} to leader {}", persistenceId(),
                            modifications.getTransactionID(), leader);
                }

                Future<Object> future = Patterns.ask(leader, modifications, TimeUnit.SECONDS.toMillis(
                        getDatastoreContext().getShardTransactionCommitTimeoutInSeconds()));

                Patterns.pipe(future, getContext().dispatcher()).pipeTo(getSelf(), ActorRef.noSender());
            }
        }
    }

    boolean hasLeader() {
        return getLeader() != null && !isIsolatedLeader();
    }

    @Override
    protected void onStateChanged() {
        super.onStateChanged();

        boolean isLeader = isLeader();
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: onStateChanged: isLeader: {}, hasLeader: {}", persistenceId(), isLeader, hasLeader());
        }

        commitCoordinator.onStateChanged(this, isLeader);
    }

    @Override
    protected void onLeaderChanged(String oldLeader, String newLeader) {
        super.onLeaderChanged(oldLeader, newLeader);

        boolean isLeader = isLeader();
        LOG.debug("{}: onLeaderChanged: oldLeader: {}, newLeader: {}, isLeader: {}", persistenceId(), oldLeader,
                newLeader, isLeader);

        if(isLeader) {

            // Clear all existing strategies so that they get re-created when we call createStrategy again
            // This allows the strategies to be re-initialized with existing statistics maintained by
            // EntityOwnershipStatistics
            strategyConfig.clearStrategies();
            // We were just elected leader. If the old leader is down, select new owners for the entities
            // owned by the down leader.

            String oldLeaderMemberName = peerIdToMemberNames.get(oldLeader);

            LOG.debug("{}: oldLeaderMemberName: {}", persistenceId(), oldLeaderMemberName);

            if(downPeerMemberNames.contains(oldLeaderMemberName)) {
                removeCandidateFromEntities(oldLeaderMemberName);
            }
        } else {
            // The leader changed - notify the coordinator to check if pending modifications need to be sent.
            // While onStateChanged also does this, this method handles the case where the shard hears from a
            // leader and stays in the follower state. In that case no behavior state change occurs.
            commitCoordinator.onStateChanged(this, isLeader);
        }
    }

    private void onCandidateRemoved(CandidateRemoved message) {
        LOG.debug("{}: onCandidateRemoved: {}", persistenceId(), message);

        if(isLeader()) {
            String currentOwner = getCurrentOwner(message.getEntityPath());
            if(message.getRemovedCandidate().equals(currentOwner) || message.getRemainingCandidates().size() == 0){
                String entityType = EntityOwnersModel.entityTypeFromEntityPath(message.getEntityPath());
                writeNewOwner(message.getEntityPath(),
                        newOwner(currentOwner, message.getRemainingCandidates(), entityOwnershipStatistics.byEntityType(entityType),
                                getEntityOwnerElectionStrategy(message.getEntityPath())));
            }
        } else {
            // We're not the leader. If the removed candidate is our local member then check if we actually
            // have a local candidate registered. If we do then we must have been partitioned from the leader
            // and the leader removed our candidate since the leader can't tell the difference between a
            // temporary network partition and a node's process actually restarted. So, in that case, re-add
            // our candidate.
            if(localMemberName.equals(message.getRemovedCandidate()) &&
                    listenerSupport.hasCandidateForEntity(createEntity(message.getEntityPath()))) {
                LOG.debug("Local candidate member was removed but a local candidate is registered for {}" +
                    " - adding back local candidate", message.getEntityPath());

                commitCoordinator.commitModification(new MergeModification(
                        candidatePath(message.getEntityPath(), localMemberName),
                        candidateMapEntry(localMemberName)), this);
            }
        }
    }

    private EntityOwnerSelectionStrategy getEntityOwnerElectionStrategy(YangInstanceIdentifier entityPath) {
        final String entityType = EntityOwnersModel.entityTypeFromEntityPath(entityPath);
        return strategyConfig.createStrategy(entityType, entityOwnershipStatistics.byEntityType(entityType));
    }

    private void onCandidateAdded(CandidateAdded message) {
        if(!isLeader()){
            return;
        }

        LOG.debug("{}: onCandidateAdded: {}", persistenceId(), message);

        // Since a node's candidate member is only added by the node itself, we can assume the node is up so
        // remove it from the downPeerMemberNames.
        downPeerMemberNames.remove(message.getNewCandidate());

        final String currentOwner = getCurrentOwner(message.getEntityPath());
        final EntityOwnerSelectionStrategy strategy = getEntityOwnerElectionStrategy(message.getEntityPath());
        final String entityType = EntityOwnersModel.entityTypeFromEntityPath(message.getEntityPath());

        // Available members is all the known peers - the number of peers that are down + self
        // So if there are 2 peers and 1 is down then availableMembers will be 2
        final int availableMembers = (peerIdToMemberNames.size() - downPeerMemberNames.size()) + 1;

        LOG.debug("{}: Using strategy {} to select owner", persistenceId(), strategy);
        if(Strings.isNullOrEmpty(currentOwner)){
            if(strategy.getSelectionDelayInMillis() == 0L) {
                writeNewOwner(message.getEntityPath(), newOwner(currentOwner, message.getAllCandidates(),
                        entityOwnershipStatistics.byEntityType(entityType), strategy));
            } else if(message.getAllCandidates().size() == availableMembers) {
                LOG.debug("{}: Received the maximum candidates requests : {} writing new owner",
                        persistenceId(), availableMembers);
                cancelOwnerSelectionTask(message.getEntityPath());
                writeNewOwner(message.getEntityPath(), newOwner(currentOwner, message.getAllCandidates(),
                        entityOwnershipStatistics.byEntityType(entityType), strategy));
            } else {
                scheduleOwnerSelection(message.getEntityPath(), message.getAllCandidates(), strategy);
            }
        }
    }

    private void onPeerDown(PeerDown peerDown) {
        LOG.info("{}: onPeerDown: {}", persistenceId(), peerDown);

        String downMemberName = peerDown.getMemberName();
        if(downPeerMemberNames.add(downMemberName) && isLeader()) {
            // Remove the down peer as a candidate from all entities.
            removeCandidateFromEntities(downMemberName);
        }
    }

    private void onPeerUp(PeerUp peerUp) {
        LOG.debug("{}: onPeerUp: {}", persistenceId(), peerUp);

        peerIdToMemberNames.put(peerUp.getPeerId(), peerUp.getMemberName());
        downPeerMemberNames.remove(peerUp.getMemberName());

        // Notify the coordinator to check if pending modifications need to be sent. We do this here
        // to handle the case where the leader's peer address isn't now yet when a prior state or
        // leader change occurred.
        commitCoordinator.onStateChanged(this, isLeader());
    }

    private void removeCandidateFromEntities(final String owner) {
        final BatchedModifications modifications = commitCoordinator.newBatchedModifications();
        searchForEntities(new EntityWalker() {
            @Override
            public void onEntity(MapEntryNode entityTypeNode, MapEntryNode entityNode) {
                if (hasCandidate(entityNode, owner)) {
                    YangInstanceIdentifier entityId =
                            (YangInstanceIdentifier) entityNode.getIdentifier().getKeyValues().get(ENTITY_ID_QNAME);
                    YangInstanceIdentifier candidatePath = candidatePath(
                            entityTypeNode.getIdentifier().getKeyValues().get(ENTITY_TYPE_QNAME).toString(),
                            entityId, owner);

                    LOG.info("{}: Found entity {}, removing candidate {}, path {}", persistenceId(), entityId,
                            owner, candidatePath);

                    modifications.addModification(new DeleteModification(candidatePath));
                }
            }
        });

        commitCoordinator.commitModifications(modifications, this);
    }

    private static boolean hasCandidate(MapEntryNode entity, String candidateName) {
        return ((MapNode)entity.getChild(CANDIDATE_NODE_ID).get()).getChild(candidateNodeKey(candidateName)).isPresent();
    }

    private void searchForEntities(EntityWalker walker) {
        Optional<NormalizedNode<?, ?>> possibleEntityTypes = getDataStore().readNode(ENTITY_TYPES_PATH);
        if(!possibleEntityTypes.isPresent()) {
            return;
        }

        for(MapEntryNode entityType:  ((MapNode) possibleEntityTypes.get()).getValue()) {
            Optional<DataContainerChild<?, ?>> possibleEntities = entityType.getChild(ENTITY_NODE_ID);
            if(!possibleEntities.isPresent()) {
                // shouldn't happen but handle anyway
                continue;
            }

            for(MapEntryNode entity:  ((MapNode) possibleEntities.get()).getValue()) {
                walker.onEntity(entityType, entity);
            }
        }
    }

    private void writeNewOwner(YangInstanceIdentifier entityPath, String newOwner) {
        LOG.debug("{}: Writing new owner {} for entity {}", persistenceId(), newOwner, entityPath);

        commitCoordinator.commitModification(new WriteModification(entityPath.node(ENTITY_OWNER_QNAME),
                ImmutableNodes.leafNode(ENTITY_OWNER_NODE_ID, newOwner)), this);
    }

    /**
     * Schedule a new owner selection job. Cancelling any outstanding job if it has not been cancelled.
     *
     * @param entityPath
     * @param allCandidates
     */
    public void scheduleOwnerSelection(YangInstanceIdentifier entityPath, Collection<String> allCandidates,
                                       EntityOwnerSelectionStrategy strategy){
        cancelOwnerSelectionTask(entityPath);

        LOG.debug("{}: Scheduling owner selection after {} ms", persistenceId(), strategy.getSelectionDelayInMillis());

        final Cancellable lastScheduledTask = context().system().scheduler().scheduleOnce(
                FiniteDuration.apply(strategy.getSelectionDelayInMillis(), TimeUnit.MILLISECONDS)
                , self(), new SelectOwner(entityPath, allCandidates, strategy)
                , context().system().dispatcher(), self());

        entityToScheduledOwnershipTask.put(entityPath, lastScheduledTask);
    }

    private void cancelOwnerSelectionTask(YangInstanceIdentifier entityPath){
        final Cancellable lastScheduledTask = entityToScheduledOwnershipTask.get(entityPath);
        if(lastScheduledTask != null && !lastScheduledTask.isCancelled()){
            lastScheduledTask.cancel();
        }
    }

    private String newOwner(String currentOwner, Collection<String> candidates, Map<String, Long> statistics, EntityOwnerSelectionStrategy ownerSelectionStrategy) {
        Collection<String> viableCandidates = getViableCandidates(candidates);
        if(viableCandidates.size() == 0){
            return "";
        }
        return ownerSelectionStrategy.newOwner(currentOwner, viableCandidates);
    }

    private Collection<String> getViableCandidates(Collection<String> candidates) {
        Collection<String> viableCandidates = new ArrayList<>();

        for (String candidate : candidates) {
            if (!downPeerMemberNames.contains(candidate)) {
                viableCandidates.add(candidate);
            }
        }
        return viableCandidates;
    }

    private String getCurrentOwner(YangInstanceIdentifier entityId) {
        Optional<NormalizedNode<?, ?>> optionalEntityOwner = getDataStore().readNode(entityId.node(ENTITY_OWNER_QNAME));
        if(optionalEntityOwner.isPresent()){
            return optionalEntityOwner.get().getValue().toString();
        }
        return null;
    }

    private static interface EntityWalker {
        void onEntity(MapEntryNode entityTypeNode, MapEntryNode entityNode);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    static class Builder extends Shard.AbstractBuilder<Builder, EntityOwnershipShard> {
        private String localMemberName;
        private EntityOwnerSelectionStrategyConfig ownerSelectionStrategyConfig;

        protected Builder() {
            super(EntityOwnershipShard.class);
        }

        Builder localMemberName(String localMemberName) {
            checkSealed();
            this.localMemberName = localMemberName;
            return this;
        }

        Builder ownerSelectionStrategyConfig(EntityOwnerSelectionStrategyConfig ownerSelectionStrategyConfig){
            checkSealed();
            this.ownerSelectionStrategyConfig = ownerSelectionStrategyConfig;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Preconditions.checkNotNull(localMemberName, "localMemberName should not be null");
            Preconditions.checkNotNull(ownerSelectionStrategyConfig, "ownerSelectionStrategyConfig should not be null");
        }
    }
}
