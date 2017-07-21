/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.mdsal.dom.spi.shard.ChildShardContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized {@link AbstractShardChangePublisher} for leaf shards, i.e. those which do not have a child shard. Unlike
 * the intermediate version, this publisher can easily reuse frontend facilities without performing additional work.
 *
 * @author Robert Varga
 */
final class LeafShardChangePublisher extends AbstractShardChangePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(LeafShardChangePublisher.class);

    @GuardedBy("this")
    private final Collection<ListenerRegistration<?>> registrations = new HashSet<>();

    LeafShardChangePublisher(final AbstractDataStore distributedDataStore, final DOMDataTreeIdentifier prefix) {
        super(distributedDataStore, prefix);
    }

    @Override
    public synchronized <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        final ListenerRegistration<org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener> frontendReg =
                registerListener(treeId, listener);

        final ListenerRegistration<L> ret = new AbstractDOMDataTreeChangeListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                frontendReg.close();
                removeReg(this);
            }
        };

        registrations.add(ret);
        return ret;
    }

    synchronized void removeReg(final ListenerRegistration<?> reg) {
        registrations.remove(reg);
    }

    @Override
    synchronized AbstractShardChangePublisher addChild(final ChildShardContext child,
            final Map<DOMDataTreeIdentifier, ChildShardContext> childShards) {
        // FIXME: implement this
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    AbstractShardChangePublisher removeChild(final ChildShardContext child,
            final Map<DOMDataTreeIdentifier, ChildShardContext> childShards) {
        throw new IllegalStateException("Attempted to remove child " + child);
    }
}
