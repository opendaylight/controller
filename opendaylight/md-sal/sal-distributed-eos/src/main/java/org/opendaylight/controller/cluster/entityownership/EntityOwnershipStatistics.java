/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityTypeFromEntityPath;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import tech.pantheon.triemap.TrieMap;

/**
 * EntityOwnershipStatistics is a utility class that keeps track of ownership statistics for the candidates and
 * caches it for quick count queries.
 * <p/>
 * While the entity ownership model does maintain the information about which entity is owned by which candidate
 * finding out how many entities of a given type are owned by a given candidate is not an efficient query.
 */
class EntityOwnershipStatistics extends AbstractEntityOwnerChangeListener {

    private final TrieMap<String, TrieMap<String, Long>> statistics = TrieMap.create();

    EntityOwnershipStatistics(){
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
        for (DataTreeCandidate change : changes) {
            DataTreeCandidateNode changeRoot = change.getRootNode();
            LeafNode<?> ownerLeaf = (LeafNode<?>) changeRoot.getDataAfter().get();
            String entityType = entityTypeFromEntityPath(change.getRootPath());
            String newOwner = extractOwner(ownerLeaf);
            if (!Strings.isNullOrEmpty(newOwner)) {
                updateStatistics(entityType, newOwner, 1);
            }

            Optional<NormalizedNode<?, ?>> dataBefore = changeRoot.getDataBefore();
            if (dataBefore.isPresent()) {
                String origOwner = extractOwner((LeafNode<?>) changeRoot.getDataBefore().get());
                if (!Strings.isNullOrEmpty(origOwner)) {
                    updateStatistics(entityType, origOwner, -1);
                }
            }
        }
    }

    Map<String, Map<String, Long>> all() {
        Map<String, Map<String, Long>> snapshot = new HashMap<>();
        for (String entityType : statistics.immutableSnapshot().keySet()) {
            snapshot.put(entityType, byEntityType(entityType));
        }
        return snapshot;
    }

    Map<String, Long> byEntityType(final String entityType) {
        if (statistics.get(entityType) != null) {
            return statistics.get(entityType).immutableSnapshot();
        }
        return new HashMap<>();
    }

    private void updateStatistics(final String entityType, final String candidateName, final long count) {
        TrieMap<String, Long> map = statistics.get(entityType);
        if (map == null) {
            map = TrieMap.create();
            map.put(candidateName, count);
            statistics.put(entityType, map);
        } else {
            map.merge(candidateName, count, (ownedEntities, addedEntities) -> ownedEntities + addedEntities);
        }
    }
}
