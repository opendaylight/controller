/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NAME_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateAdded;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.CandidateRemoved;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for candidate entries added/removed and notifies the EntityOwnershipShard appropriately.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
class CandidateListChangeListener implements DOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(CandidateListChangeListener.class);

    private final String logId;
    private final ActorRef shard;
    private final Map<YangInstanceIdentifier, Collection<String>> currentCandidates = new HashMap<>();

    CandidateListChangeListener(ActorRef shard, String logId) {
        this.shard = Preconditions.checkNotNull(shard, "shard should not be null");
        this.logId = logId;
    }

    void init(ShardDataTree shardDataTree) {
        shardDataTree.registerTreeChangeListener(YangInstanceIdentifier.builder(ENTITY_OWNERS_PATH).
                node(EntityType.QNAME).node(EntityType.QNAME).node(ENTITY_QNAME).node(ENTITY_QNAME).
                        node(Candidate.QNAME).node(Candidate.QNAME).build(), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
        for(DataTreeCandidate change: changes) {
            DataTreeCandidateNode changeRoot = change.getRootNode();
            ModificationType type = changeRoot.getModificationType();

            LOG.debug("{}: Candidate node changed: {}, {}", logId, type, change.getRootPath());

            NodeIdentifierWithPredicates candidateKey =
                    (NodeIdentifierWithPredicates) change.getRootPath().getLastPathArgument();
            String candidate = candidateKey.getKeyValues().get(CANDIDATE_NAME_QNAME).toString();

            YangInstanceIdentifier entityId = extractEntityPath(change.getRootPath());

            if(type == ModificationType.WRITE || type == ModificationType.APPEARED) {
                LOG.debug("{}: Candidate {} was added for entity {}", logId, candidate, entityId);

                Collection<String> currentCandidates = addToCurrentCandidates(entityId, candidate);
                shard.tell(new CandidateAdded(entityId, candidate, new ArrayList<>(currentCandidates)), shard);
            } else if(type == ModificationType.DELETE || type == ModificationType.DISAPPEARED) {
                LOG.debug("{}: Candidate {} was removed for entity {}", logId, candidate, entityId);

                Collection<String> currentCandidates = removeFromCurrentCandidates(entityId, candidate);
                shard.tell(new CandidateRemoved(entityId, candidate, new ArrayList<>(currentCandidates)), shard);
            }
        }
    }

    private Collection<String> addToCurrentCandidates(YangInstanceIdentifier entityId, String newCandidate) {
        Collection<String> candidates = currentCandidates.get(entityId);
        if(candidates == null) {
            candidates = new LinkedHashSet<>();
            currentCandidates.put(entityId, candidates);
        }

        candidates.add(newCandidate);
        return candidates;
    }

    private Collection<String> removeFromCurrentCandidates(YangInstanceIdentifier entityId, String candidateToRemove) {
        Collection<String> candidates = currentCandidates.get(entityId);
        if(candidates != null) {
            candidates.remove(candidateToRemove);
            return candidates;
        }

        // Shouldn't happen
        return Collections.emptyList();
    }

    private static YangInstanceIdentifier extractEntityPath(YangInstanceIdentifier candidatePath) {
        List<PathArgument> newPathArgs = new ArrayList<>();
        for(PathArgument pathArg: candidatePath.getPathArguments()) {
            newPathArgs.add(pathArg);
            if(pathArg instanceof NodeIdentifierWithPredicates) {
                NodeIdentifierWithPredicates nodeKey = (NodeIdentifierWithPredicates) pathArg;
                Entry<QName, Object> key = nodeKey.getKeyValues().entrySet().iterator().next();
                if(ENTITY_ID_QNAME.equals(key.getKey())) {
                    break;
                }
            }
        }

        return YangInstanceIdentifier.create(newPathArgs);
    }
}
