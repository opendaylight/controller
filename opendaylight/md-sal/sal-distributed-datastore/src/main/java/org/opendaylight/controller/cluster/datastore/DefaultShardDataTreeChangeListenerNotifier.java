/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.dom.store.impl.ResolveDataChangeEventsTask;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of ShardDataTreeChangeListenerNotifier.
 *
 * @author Thomas Pantelis
 */
class DefaultShardDataTreeChangeListenerNotifier implements ShardDataTreeChangeListenerNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultShardDataTreeChangeListenerNotifier.class);

    private ShardDataTreeChangePublisher treeChangePublisher;
    private ListenerTree dataChangeListenerTree;

    @Override
    public void init(final ShardDataTreeChangePublisher treeChangePublisher,
            final ListenerTree dataChangeListenerTree) {
        this.treeChangePublisher = Preconditions.checkNotNull(treeChangePublisher);
        this.dataChangeListenerTree = Preconditions.checkNotNull(dataChangeListenerTree);
    }

    @Override
    public void notifyListeners(final DataTreeCandidate candidate) {
        LOG.debug("Notifying listeners on candidate {}", candidate);

        // DataTreeChanges first, as they are more light-weight
        treeChangePublisher.publishChanges(candidate);

        // DataChanges second, as they are heavier
        ResolveDataChangeEventsTask.create(candidate, dataChangeListenerTree).resolve(ShardDataTree.MANAGER);
    }
}
