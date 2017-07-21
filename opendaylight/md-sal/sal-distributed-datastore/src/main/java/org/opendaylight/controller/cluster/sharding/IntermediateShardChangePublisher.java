/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import java.util.Map;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.ChildShardContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized class for shards which have child shards.
 *
 * @author Robert Varga
 */
final class IntermediateShardChangePublisher extends AbstractShardChangePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(IntermediateShardChangePublisher.class);

    IntermediateShardChangePublisher(final AbstractDataStore frontend, final DOMDataTreeIdentifier prefix) {
        super(frontend, prefix);
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    AbstractShardChangePublisher addChild(final ChildShardContext child,
            final Map<DOMDataTreeIdentifier, ChildShardContext> childShards) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    AbstractShardChangePublisher removeChild(final ChildShardContext child,
            final Map<DOMDataTreeIdentifier, ChildShardContext> childShards) {
        if (childShards.isEmpty()) {
            // FIXME: convert state into a LeafShardChangePublisher

            return null;
        }

        // FIXME: adjust internal state

        return this;
    }
}
