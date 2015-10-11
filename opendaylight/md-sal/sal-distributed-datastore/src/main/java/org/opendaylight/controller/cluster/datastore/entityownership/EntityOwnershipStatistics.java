/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityTypeFromEntityPath;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.romix.scala.collection.concurrent.TrieMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

/**
 * EntityOwnershipStatistics is a utility class that keeps track of ownership statistics for the candidates and
 * caches it for quick count queries.
 * <p>
 * While the entity ownership model does maintain the information about which entity is owned by which candidate
 * finding out how many entities of a given type are owned by a given candidate is not an efficient query.
 */
class EntityOwnershipStatistics extends AbstractEntityOwnerChangeListener {

    private TrieMap<String, TrieMap<String, Long>> statistics = new TrieMap<>();

    EntityOwnershipStatistics(){
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeCandidate> changes) {
        for (DataTreeCandidate change : changes) {
            DataTreeCandidateNode changeRoot = change.getRootNode();
            LeafNode<?> ownerLeaf = (LeafNode<?>) changeRoot.getDataAfter().get();
            String entityType = entityTypeFromEntityPath(change.getRootPath());
            String newOwner = extractOwner(ownerLeaf);
            if(!Strings.isNullOrEmpty(newOwner)) {
                updateStatistics(entityType, newOwner, 1);
            }

            Optional<NormalizedNode<?, ?>> dataBefore = changeRoot.getDataBefore();
            if (dataBefore.isPresent()) {
                String origOwner = extractOwner((LeafNode<?>) changeRoot.getDataBefore().get());
                if(!Strings.isNullOrEmpty(origOwner)) {
                    updateStatistics(entityType, origOwner, -1);
                }
            }
        }
    }

    Map<String, Map<String, Long>> all() {
        Map<String, Map<String, Long>> snapshot = new HashMap<>();
        for (String entityType : statistics.readOnlySnapshot().keySet()) {
            snapshot.put(entityType, byEntityType(entityType));
        }
        return snapshot;
    }

    Map<String, Long> byEntityType(String entityType){
        if(statistics.get(entityType) != null) {
            return statistics.get(entityType).readOnlySnapshot();
        }
        return new HashMap<>();
    }

    private void updateStatistics(String entityType, String candidateName, long count){
        Map<String, Long> m = statistics.get(entityType);
        if(m == null){
            m = new TrieMap<>();
            m.put(candidateName, count);
            statistics.put(entityType, m);
        } else {
            Long candidateOwnedEntities = m.get(candidateName);
            if(candidateOwnedEntities == null){
                m.put(candidateName, count);
            } else {
                m.put(candidateName, candidateOwnedEntities + count);
            }
        }
    }
}