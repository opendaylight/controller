/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NAME_NODE_ID;
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
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidateNodeKey;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidatePath;
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
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateAdded;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateRemoved;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RemoveAllCandidates;
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
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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
    private final MemberName localMemberName;
    private final EntityOwnershipShardCommitCoordinator commitCoordinator;
    private final EntityOwnershipListenerSupport listenerSupport;
    private final Set<MemberName> downPeerMemberNames = new HashSet<>();
    private final Map<String, MemberName> peerIdToMemberNames = new HashMap<>();
    private final EntityOwnerSelectionStrategyConfig strategyConfig;
    private final Map<YangInstanceIdentifier, Cancellable> entityToScheduledOwnershipTask = new HashMap<>();
    private final EntityOwnershipStatistics entityOwnershipStatistics;
    private boolean removeAllInitialCandidates = true;

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
            ShardIdentifier shardId = ShardIdentifier.fromShardIdString(peerId);
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
    public void handleNonRaftCommand(final Object message) {
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
        } else if(message instanceof RemoveAllCandidates) {
            onRemoveAllCandidates((RemoveAllCandidates) message);
        } else if(!commitCoordinator.handleMessage(message, this)) {
            super.handleNonRaftCommand(message);
        }
    }

    private void onRemoveAllCandidates(RemoveAllCandidates message) {
        LOG.debug("{}: onRemoveAllCandidates: {}", persistenceId(), message);

        removeCandidateFromEntities(message.getMemberName());
    }

    private void onSelectOwner(SelectOwner selectOwner) {
        LOG.debug("{}: onSelectOwner: {}", persistenceId(), selectOwner);

        String currentOwner = getCurrentOwner(selectOwner.getEntityPath());
        if(Strings.isNullOrEmpty(currentOwner)) {
            writeNewOwner(selectOwner.getEntityPath(), newOwner(currentOwner, selectOwner.getAllCandidates(),
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
                registerCandidate.getEntity().getIdentifier(), localMemberName.getName());
        commitCoordinator.commitModification(new MergeModification(ENTITY_OWNERS_PATH, entityOwners), this);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    private void onUnregisterCandidateLocal(UnregisterCandidateLocal unregisterCandidate) {
        LOG.debug("{}: onUnregisterCandidateLocal: {}", persistenceId(), unregisterCandidate);

        DOMEntity entity = unregisterCandidate.getEntity();
        listenerSupport.unsetHasCandidateForEntity(entity);

        YangInstanceIdentifier candidatePath = candidatePath(entity.getType(), entity.getIdentifier(), localMemberName.getName());
        commitCoordinator.commitModification(new DeleteModification(candidatePath), this);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    private void onRegisterListenerLocal(final RegisterListenerLocal registerListener) {
        LOG.debug("{}: onRegisterListenerLocal: {}", persistenceId(), registerListener);

        listenerSupport.addEntityOwnershipListener(registerListener.getEntityType(), registerListener.getListener());

        getSender().tell(SuccessReply.INSTANCE, getSelf());

        searchForEntities((entityTypeNode, entityNode) -> {
            Optional<DataContainerChild<?, ?>> possibleType = entityTypeNode.getChild(ENTITY_TYPE_NODE_ID);
            String entityType = possibleType.isPresent() ? possibleType.get().getValue().toString() : null;
            if (registerListener.getEntityType().equals(entityType)) {
                final boolean hasOwner;
                final boolean isOwner;

                Optional<DataContainerChild<?, ?>> possibleOwner = entityNode.getChild(ENTITY_OWNER_NODE_ID);
                if (possibleOwner.isPresent()) {
                    isOwner = localMemberName.getName().equals(possibleOwner.get().getValue().toString());
                    hasOwner = true;
                } else {
                    isOwner = false;
                    hasOwner = false;
                }

                DOMEntity entity = new DOMEntity(entityType,
                    (YangInstanceIdentifier) entityNode.getChild(ENTITY_ID_NODE_ID).get().getValue());

                listenerSupport.notifyEntityOwnershipListener(entity, false, isOwner, hasOwner,
                    registerListener.getListener());
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
                possiblyRemoveAllInitialCandidates(leader);

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

    void possiblyRemoveAllInitialCandidates(ActorSelection leader) {
        // The following handles removing all candidates on startup when re-joining with a remote leader. When a
        // follower is detected as down, the leader will re-assign new owners to entities that were owned by the
        // down member but doesn't remove the down member as a candidate, as the down node may actually be isolated
        // and still running. Therefore on startup we send an initial message to the remote leader to remove any
        // potential stale candidates we had previously registered, as it's possible a candidate may not be
        // registered by a client in the new incarnation. We have to send the RemoveAllCandidates message prior to any
        // pending registrations.
        if(removeAllInitialCandidates && leader != null) {
            removeAllInitialCandidates = false;
            if(!isLeader()) {
                LOG.debug("{} - got new leader {} on startup - sending RemoveAllCandidates", persistenceId(), leader);

                leader.tell(new RemoveAllCandidates(localMemberName), ActorRef.noSender());
            }
        }
    }

    boolean hasLeader() {
        return getLeader() != null && (!isLeader() || isLeaderActive());
    }

    /**
     * Determine if we are in jeopardy based on observed RAFT state.
     */
    private static boolean inJeopardy(final RaftState state) {
        switch (state) {
            case Candidate:
            case Follower:
            case Leader:
            case PreLeader:
                return false;
            case IsolatedLeader:
                return true;
        }
        throw new IllegalStateException("Unsupported RAFT state " + state);
    }

    private void notifyAllListeners() {
        searchForEntities((entityTypeNode, entityNode) -> {
            Optional<DataContainerChild<?, ?>> possibleType = entityTypeNode.getChild(ENTITY_TYPE_NODE_ID);
            if (possibleType.isPresent()) {
                final boolean hasOwner;
                final boolean isOwner;

                Optional<DataContainerChild<?, ?>> possibleOwner = entityNode.getChild(ENTITY_OWNER_NODE_ID);
                if (possibleOwner.isPresent()) {
                    isOwner = localMemberName.getName().equals(possibleOwner.get().getValue().toString());
                    hasOwner = true;
                } else {
                    isOwner = false;
                    hasOwner = false;
                }

                DOMEntity entity = new DOMEntity(possibleType.get().getValue().toString(),
                    (YangInstanceIdentifier) entityNode.getChild(ENTITY_ID_NODE_ID).get().getValue());

                listenerSupport.notifyEntityOwnershipListeners(entity, isOwner, isOwner, hasOwner);
            }
        });
    }

    @Override
    protected void onStateChanged() {
        super.onStateChanged();

        boolean isLeader = isLeader();
        LOG.debug("{}: onStateChanged: isLeader: {}, hasLeader: {}", persistenceId(), isLeader, hasLeader());

        // Examine current RAFT state to see if we are in jeopardy, potentially notifying all listeners
        final boolean inJeopardy = inJeopardy(getRaftState());
        final boolean wasInJeopardy = listenerSupport.setInJeopardy(inJeopardy);
        if (inJeopardy != wasInJeopardy) {
            LOG.debug("{}: {} jeopardy state, notifying all listeners", persistenceId(),
                inJeopardy ? "entered" : "left");
            notifyAllListeners();
        }

        commitCoordinator.onStateChanged(this, isLeader);
    }

    @Override
    protected void onLeaderChanged(String oldLeader, String newLeader) {
        super.onLeaderChanged(oldLeader, newLeader);

        boolean isLeader = isLeader();
        LOG.debug("{}: onLeaderChanged: oldLeader: {}, newLeader: {}, isLeader: {}", persistenceId(), oldLeader,
                newLeader, isLeader);

        if (isLeader) {

            // Clear all existing strategies so that they get re-created when we call createStrategy again
            // This allows the strategies to be re-initialized with existing statistics maintained by
            // EntityOwnershipStatistics
            strategyConfig.clearStrategies();

            // Re-assign owners for all members that are known to be down. In a cluster which has greater than
            // 3 nodes it is possible for a some node beside the leader being down when the leadership transitions
            // it makes sense to use this event to re-assign owners for those downed nodes
            for (MemberName downPeerName : downPeerMemberNames) {
                selectNewOwnerForEntitiesOwnedBy(downPeerName);
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
            if(message.getRemovedCandidate().equals(currentOwner) || message.getRemainingCandidates().isEmpty()){
                writeNewOwner(message.getEntityPath(),
                        newOwner(currentOwner, message.getRemainingCandidates(), getEntityOwnerElectionStrategy(message.getEntityPath())));
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
        downPeerMemberNames.remove(MemberName.forName(message.getNewCandidate()));

        final String currentOwner = getCurrentOwner(message.getEntityPath());
        final EntityOwnerSelectionStrategy strategy = getEntityOwnerElectionStrategy(message.getEntityPath());

        // Available members is all the known peers - the number of peers that are down + self
        // So if there are 2 peers and 1 is down then availableMembers will be 2
        final int availableMembers = peerIdToMemberNames.size() - downPeerMemberNames.size() + 1;

        LOG.debug("{}: Using strategy {} to select owner, currentOwner = {}", persistenceId(), strategy, currentOwner);

        if(strategy.getSelectionDelayInMillis() == 0L) {
            writeNewOwner(message.getEntityPath(), newOwner(currentOwner, message.getAllCandidates(),
                    strategy));
        } else if(message.getAllCandidates().size() == availableMembers) {
            LOG.debug("{}: Received the maximum candidates requests : {} writing new owner",
                    persistenceId(), availableMembers);
            cancelOwnerSelectionTask(message.getEntityPath());
            writeNewOwner(message.getEntityPath(), newOwner(currentOwner, message.getAllCandidates(),
                    strategy));
        } else {
            scheduleOwnerSelection(message.getEntityPath(), message.getAllCandidates(), strategy);
        }
    }

    private void onPeerDown(PeerDown peerDown) {
        LOG.info("{}: onPeerDown: {}", persistenceId(), peerDown);

        MemberName downMemberName = peerDown.getMemberName();
        if(downPeerMemberNames.add(downMemberName) && isLeader()) {
            // Select new owners for entities owned by the down peer and which have other candidates. For an entity for
            // which the down peer is the only candidate, we leave it as the owner and don't clear it. This is done to
            // handle the case where the peer member process is actually still running but the node is partitioned.
            // When the partition is healed, the peer just remains as the owner. If the peer process actually restarted,
            // it will first remove all its candidates on startup. If another candidate is registered during the time
            // the peer is down, the new candidate will be selected as the new owner.

            selectNewOwnerForEntitiesOwnedBy(downMemberName);
        }
    }

    private void selectNewOwnerForEntitiesOwnedBy(MemberName downMemberName) {
        final BatchedModifications modifications = commitCoordinator.newBatchedModifications();
        searchForEntitiesOwnedBy(downMemberName.getName(), (entityTypeNode, entityNode) -> {
            YangInstanceIdentifier entityPath = YangInstanceIdentifier.builder(ENTITY_TYPES_PATH).
                    node(entityTypeNode.getIdentifier()).node(ENTITY_NODE_ID).node(entityNode.getIdentifier()).
                    node(ENTITY_OWNER_NODE_ID).build();
            String newOwner = newOwner(getCurrentOwner(entityPath), getCandidateNames(entityNode),
                    getEntityOwnerElectionStrategy(entityPath));

            if(!newOwner.isEmpty()) {
                LOG.debug("{}: Found entity {}, writing new owner {}", persistenceId(), entityPath, newOwner);

                modifications.addModification(new WriteModification(entityPath,
                        ImmutableNodes.leafNode(ENTITY_OWNER_NODE_ID, newOwner)));

            } else {
                LOG.debug("{}: Found entity {} but no other candidates - not clearing owner", persistenceId(),
                        entityPath, newOwner);
            }
        });

        commitCoordinator.commitModifications(modifications, this);
    }

    private void onPeerUp(PeerUp peerUp) {
        LOG.debug("{}: onPeerUp: {}", persistenceId(), peerUp);

        peerIdToMemberNames.put(peerUp.getPeerId(), peerUp.getMemberName());
        downPeerMemberNames.remove(peerUp.getMemberName());

        // Notify the coordinator to check if pending modifications need to be sent. We do this here
        // to handle the case where the leader's peer address isn't known yet when a prior state or
        // leader change occurred.
        commitCoordinator.onStateChanged(this, isLeader());
    }

    private Collection<String> getCandidateNames(MapEntryNode entity) {
        Collection<MapEntryNode> candidates = ((MapNode)entity.getChild(CANDIDATE_NODE_ID).get()).getValue();
        Collection<String> candidateNames = new ArrayList<>(candidates.size());
        for(MapEntryNode candidate: candidates) {
            candidateNames.add(candidate.getChild(CANDIDATE_NAME_NODE_ID).get().getValue().toString());
        }

        return candidateNames;
    }

    private void searchForEntitiesOwnedBy(final String owner, final EntityWalker walker) {
        LOG.debug("{}: Searching for entities owned by {}", persistenceId(), owner);

        searchForEntities((entityTypeNode, entityNode) -> {
            Optional<DataContainerChild<? extends PathArgument, ?>> possibleOwner =
                    entityNode.getChild(ENTITY_OWNER_NODE_ID);
            if(possibleOwner.isPresent() && owner.equals(possibleOwner.get().getValue().toString())) {
                walker.onEntity(entityTypeNode, entityNode);
            }
        });
    }

    private void removeCandidateFromEntities(final MemberName member) {
        final BatchedModifications modifications = commitCoordinator.newBatchedModifications();
        searchForEntities((entityTypeNode, entityNode) -> {
            if (hasCandidate(entityNode, member)) {
                YangInstanceIdentifier entityId =
                        (YangInstanceIdentifier) entityNode.getIdentifier().getKeyValues().get(ENTITY_ID_QNAME);
                YangInstanceIdentifier candidatePath = candidatePath(
                        entityTypeNode.getIdentifier().getKeyValues().get(ENTITY_TYPE_QNAME).toString(),
                        entityId, member.getName());

                LOG.info("{}: Found entity {}, removing candidate {}, path {}", persistenceId(), entityId,
                        member, candidatePath);

                modifications.addModification(new DeleteModification(candidatePath));
            }
        });

        commitCoordinator.commitModifications(modifications, this);
    }

    private static boolean hasCandidate(MapEntryNode entity, MemberName candidateName) {
        return ((MapNode)entity.getChild(CANDIDATE_NODE_ID).get()).getChild(candidateNodeKey(candidateName.getName()))
                .isPresent();
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

    private String newOwner(String currentOwner, Collection<String> candidates, EntityOwnerSelectionStrategy ownerSelectionStrategy) {
        Collection<String> viableCandidates = getViableCandidates(candidates);
        if(viableCandidates.isEmpty()){
            return "";
        }
        return ownerSelectionStrategy.newOwner(currentOwner, viableCandidates);
    }

    private Collection<String> getViableCandidates(Collection<String> candidates) {
        Collection<String> viableCandidates = new ArrayList<>();

        for (String candidate : candidates) {
            if (!downPeerMemberNames.contains(MemberName.forName(candidate))) {
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

    @FunctionalInterface
    private static interface EntityWalker {
        void onEntity(MapEntryNode entityTypeNode, MapEntryNode entityNode);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    static class Builder extends Shard.AbstractBuilder<Builder, EntityOwnershipShard> {
        private MemberName localMemberName;
        private EntityOwnerSelectionStrategyConfig ownerSelectionStrategyConfig;

        protected Builder() {
            super(EntityOwnershipShard.class);
        }

        Builder localMemberName(MemberName localMemberName) {
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
