/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import akka.pattern.Patterns;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.SuccessReply;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.EntityOwners;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Future;

/**
 * Special Shard for EntityOwnership.
 *
 * @author Thomas Pantelis
 */
class EntityOwnershipShard extends Shard {
    static final YangInstanceIdentifier ENTITY_OWNERS_PATH = YangInstanceIdentifier.of(EntityOwners.QNAME);
    static final  QName ENTITY_QNAME = org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.
            md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.Entity.QNAME;
    static final QName CANDIDATE_NAME = QName.create(Candidate.QNAME, "name");
    static final QName ENTITY_ID = QName.create(ENTITY_QNAME, "id");
    static final QName ENTITY_TYPE = QName.create(EntityType.QNAME, "type");

    private int transactionIDCounter = 0;
    private final String localMemberName;
    private final List<BatchedModifications> retryModifications = new ArrayList<>();

    private static DatastoreContext noPersistenceDatastoreContext(DatastoreContext datastoreContext) {
        return DatastoreContext.newBuilderFrom(datastoreContext).persistent(false).build();
    }

    protected EntityOwnershipShard(ShardIdentifier name, Map<String, String> peerAddresses,
            DatastoreContext datastoreContext, SchemaContext schemaContext, String localMemberName) {
        super(name, peerAddresses, noPersistenceDatastoreContext(datastoreContext), schemaContext);
        this.localMemberName = localMemberName;
    }

    @Override
    protected void onDatastoreContext(DatastoreContext context) {
        super.onDatastoreContext(noPersistenceDatastoreContext(context));
    }

    @Override
    public void onReceiveCommand(final Object message) throws Exception {
        if(message instanceof RegisterCandidateLocal) {
            onRegisterCandidateLocal((RegisterCandidateLocal)message);
        } else if(message instanceof UnregisterCandidateLocal) {
            onUnregisterCandidateLocal((UnregisterCandidateLocal)message);
        } else {
            super.onReceiveCommand(message);
        }
    }

    private void onRegisterCandidateLocal(RegisterCandidateLocal registerCandidate) {
        LOG.debug("onRegisterCandidateLocal: {}", registerCandidate);

        // TODO - add the listener locally.

        BatchedModifications modifications = new BatchedModifications(
                TransactionIdentifier.create(localMemberName, ++transactionIDCounter).toString(),
                DataStoreVersions.CURRENT_VERSION, "");
        modifications.setDoCommitOnReady(true);
        modifications.setReady(true);
        modifications.setTotalMessagesSent(1);

        NormalizedNode<?, ?> entityOwners = createEntityOwnersWithCandidate(registerCandidate.getEntity(), localMemberName);
        modifications.addModification(new MergeModification(ENTITY_OWNERS_PATH, entityOwners));

        tryCommitModifications(modifications);

        getSender().tell(SuccessReply.INSTANCE, getSelf());
    }

    private NormalizedNode<?, ?> createEntityOwnersWithCandidate(Entity entity, String memberName) {
        MapNode candidateNode = ImmutableNodes.mapNodeBuilder(Candidate.QNAME).addChild(
                        ImmutableNodes.mapEntry(Candidate.QNAME, CANDIDATE_NAME, memberName)).build();

        MapEntryNode entityNode = ImmutableNodes.mapEntryBuilder(ENTITY_QNAME, ENTITY_ID, entity.getId()).
                addChild(candidateNode).build();

        MapEntryNode entityTypeNode = ImmutableNodes.mapEntryBuilder(EntityType.QNAME, ENTITY_TYPE, entity.getType()).
                addChild(ImmutableNodes.mapNodeBuilder(ENTITY_QNAME).addChild(entityNode).build()).build();

        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(EntityOwners.QNAME)).
                addChild(ImmutableNodes.mapNodeBuilder(EntityType.QNAME).addChild(entityTypeNode).build()).build();
    }

    private void tryCommitModifications(final BatchedModifications modifications) {
        if(isLeader()) {
            if(isIsolatedLeader()) {
                LOG.debug("Leader is isolated - adding BatchedModifications {} for retry", modifications.getTransactionID());

                retryModifications.add(modifications);
            } else {
                LOG.debug("Committing BatchedModifications {} locally", modifications.getTransactionID());

                // Note that it's possible the commit won't get consensus and will timeout and not be applied
                // to the state. However we don't need to retry it in that case b/c it will be committed to
                // the journal first and, once a majority of followers come back on line and it is replicated,
                // it will be applied at that point.
                handleBatchedModificationsLocal(modifications, self());
            }
        } else {
            final ActorSelection leader = getLeader();
            if (leader != null) {
                LOG.debug("Sending BatchedModifications {} to leader {}", modifications.getTransactionID(), leader);

                Future<Object> future = Patterns.ask(leader, modifications, TimeUnit.SECONDS.toMillis(
                        getDatastoreContext().getShardTransactionCommitTimeoutInSeconds()));
                future.onComplete(new OnComplete<Object>() {
                    @Override
                    public void onComplete(Throwable failure, Object response) {
                        if(failure != null) {
                            if(failure instanceof AskTimeoutException) {
                                LOG.debug("BatchedModifications {} to leader {} timed out - retrying",
                                        modifications.getTransactionID(), leader);
                                tryCommitModifications(modifications);
                            } else {
                                LOG.error("BatchedModifications {} to leader {} failed",
                                        modifications.getTransactionID(), leader, failure);
                            }
                        } else {
                            LOG.debug("BatchedModifications {} to leader {} succeeded",
                                    modifications.getTransactionID(), leader);
                        }
                    }
                }, getContext().dispatcher());
            } else {
                LOG.debug("No leader - adding BatchedModifications {} for retry", modifications.getTransactionID());

                retryModifications.add(modifications);
            }
        }
    }

    @Override
    protected void onStateChanged() {
        super.onStateChanged();

        if(!retryModifications.isEmpty() && getLeader() != null && !isIsolatedLeader()) {
            LOG.debug("# BatchedModifications to retry {}", retryModifications.size());

            List<BatchedModifications> retryModificationsCopy = new ArrayList<>(retryModifications);
            retryModifications.clear();
            for(BatchedModifications mods: retryModificationsCopy) {
                tryCommitModifications(mods);
            }
        }
    }

    private void onUnregisterCandidateLocal(UnregisterCandidateLocal unregisterCandidate) {
        // TODO - implement
        getSender().tell(SuccessReply.INSTANCE, getSelf());
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
