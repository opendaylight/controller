/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.pattern.Patterns;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateAdded;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateRemoved;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Future;

/**
 * Special Shard for EntityOwnership.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipShard extends Shard {
    private final String localMemberName;
    private final EntityOwnershipShardCommitCoordinator commitCoordinator;
    private final EntityOwnershipListenerSupport listenerSupport;

    private static DatastoreContext noPersistenceDatastoreContext(DatastoreContext datastoreContext) {
        return DatastoreContext.newBuilderFrom(datastoreContext).persistent(false).build();
    }

    protected EntityOwnershipShard(ShardIdentifier name, Map<String, String> peerAddresses,
            DatastoreContext datastoreContext, SchemaContext schemaContext, String localMemberName) {
        super(name, peerAddresses, noPersistenceDatastoreContext(datastoreContext), schemaContext);
        this.localMemberName = localMemberName;
        this.commitCoordinator = new EntityOwnershipShardCommitCoordinator(localMemberName, LOG);
        this.listenerSupport = new EntityOwnershipListenerSupport(getContext());
    }

    @Override
    protected void onDatastoreContext(DatastoreContext context) {
        super.onDatastoreContext(noPersistenceDatastoreContext(context));
    }

    @Override
    protected void onRecoveryComplete() {
        super.onRecoveryComplete();

        new CandidateListChangeListener(getSelf()).init(getDataStore());
        new EntityOwnerChangeListener(localMemberName, listenerSupport).init(getDataStore());
    }

    @Override
    public void onReceiveCommand(final Object message) throws Exception {
        if(message instanceof RegisterCandidateLocal) {
            onRegisterCandidateLocal((RegisterCandidateLocal)message);
        } else if(message instanceof UnregisterCandidateLocal) {
            onUnregisterCandidateLocal((UnregisterCandidateLocal)message);
        } else if(message instanceof CandidateAdded){
            onCandidateAdded((CandidateAdded) message);
        } else if(message instanceof CandidateRemoved){
            onCandidateRemoved((CandidateRemoved) message);
        } else if(!commitCoordinator.handleMessage(message, this)) {
            super.onReceiveCommand(message);
        }
    }

    private void onRegisterCandidateLocal(RegisterCandidateLocal registerCandidate) {
        LOG.debug("onRegisterCandidateLocal: {}", registerCandidate);

        listenerSupport.addEntityOwnershipListener(registerCandidate.getEntity(), registerCandidate.getCandidate());

        NormalizedNode<?, ?> entityOwners = entityOwnersWithCandidate(registerCandidate.getEntity().getType(),
                registerCandidate.getEntity().getId(), localMemberName);
        commitCoordinator.commitModification(new MergeModification(ENTITY_OWNERS_PATH, entityOwners), this);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    private void onUnregisterCandidateLocal(UnregisterCandidateLocal unregisterCandidate) {
        LOG.debug("onUnregisterCandidateLocal: {}", unregisterCandidate);

        Entity entity = unregisterCandidate.getEntity();
        listenerSupport.removeEntityOwnershipListener(entity, unregisterCandidate.getCandidate());

        YangInstanceIdentifier candidatePath = candidatePath(entity.getType(), entity.getId(), localMemberName);
        commitCoordinator.commitModification(new DeleteModification(candidatePath), this);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    void tryCommitModifications(final BatchedModifications modifications) {
        if(isLeader()) {
            LOG.debug("Committing BatchedModifications {} locally", modifications.getTransactionID());

            // Note that it's possible the commit won't get consensus and will timeout and not be applied
            // to the state. However we don't need to retry it in that case b/c it will be committed to
            // the journal first and, once a majority of followers come back on line and it is replicated,
            // it will be applied at that point.
            handleBatchedModificationsLocal(modifications, self());
        } else {
            final ActorSelection leader = getLeader();
            if (leader != null) {
                LOG.debug("Sending BatchedModifications {} to leader {}", modifications.getTransactionID(), leader);

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

        commitCoordinator.onStateChanged(this, isLeader());
    }

    private void onCandidateRemoved(CandidateRemoved message) {
        if(!isLeader()){
            return;
        }

        LOG.debug("onCandidateRemoved: {}", message);

        String currentOwner = getCurrentOwner(message.getEntityPath());
        if(message.getRemovedCandidate().equals(currentOwner)){
            writeNewOwner(message.getEntityPath(), newOwner(message.getRemainingCandidates()));
        }
    }

    private void onCandidateAdded(CandidateAdded message) {
        if(!isLeader()){
            return;
        }

        LOG.debug("onCandidateAdded: {}", message);

        String currentOwner = getCurrentOwner(message.getEntityPath());
        if(Strings.isNullOrEmpty(currentOwner)){
            writeNewOwner(message.getEntityPath(), newOwner(message.getAllCandidates()));
        }
    }

    private void writeNewOwner(YangInstanceIdentifier entityPath, String newOwner) {
        LOG.debug("Writing new owner {} for entity {}", newOwner, entityPath);

        commitCoordinator.commitModification(new WriteModification(entityPath.node(ENTITY_OWNER_QNAME),
                ImmutableNodes.leafNode(ENTITY_OWNER_NODE_ID, newOwner)), this);
    }

    private String newOwner(Collection<String> candidates) {
        if(candidates.size() > 0){
            return candidates.iterator().next();
        }

        return "";
    }

    private String getCurrentOwner(YangInstanceIdentifier entityId) {
        DataTreeSnapshot snapshot = getDataStore().getDataTree().takeSnapshot();
        Optional<NormalizedNode<?, ?>> optionalEntityOwner = snapshot.readNode(entityId.node(ENTITY_OWNER_QNAME));
        if(optionalEntityOwner.isPresent()){
            return optionalEntityOwner.get().getValue().toString();
        }
        return null;
    }

    public static Props props(final ShardIdentifier name, final Map<String, String> peerAddresses,
            final DatastoreContext datastoreContext, final SchemaContext schemaContext, final String localMemberName) {
        return Props.create(new Creator(name, peerAddresses, datastoreContext, schemaContext, localMemberName));
    }

    private static class Creator extends AbstractShardCreator {
        private static final long serialVersionUID = 1L;

        private final String localMemberName;

        Creator(final ShardIdentifier name, final Map<String, String> peerAddresses,
                final DatastoreContext datastoreContext, final SchemaContext schemaContext,
                final String localMemberName) {
            super(name, peerAddresses, datastoreContext, schemaContext);
            this.localMemberName = localMemberName;
        }

        @Override
        public Shard create() throws Exception {
            return new EntityOwnershipShard(name, peerAddresses, datastoreContext, schemaContext, localMemberName);
        }
    }
}
