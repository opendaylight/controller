/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

import com.google.common.base.Preconditions;

abstract class AbstractStatsTracker<I, K> {
    private final Map<K, Long> trackedItems = new HashMap<>();
    private final InstanceIdentifier<Node> nodeIdentifier;
    private final DataProviderService dps;
    private final long lifetimeNanos;

    protected AbstractStatsTracker(final InstanceIdentifier<Node> nodeIdentifier, final DataProviderService dps, long lifetimeNanos) {
        this.nodeIdentifier = Preconditions.checkNotNull(nodeIdentifier);
        this.dps = Preconditions.checkNotNull(dps);
        this.lifetimeNanos = lifetimeNanos;
    }

    protected final InstanceIdentifierBuilder<Node> getNodeIdentifierBuilder() {
        return InstanceIdentifier.builder(nodeIdentifier);
    }

    final synchronized void updateStats(List<I> list) {
        final Long expiryTime = System.nanoTime() + lifetimeNanos;
        final DataModificationTransaction trans = dps.beginTransaction();

        for (final I item : list) {
            trackedItems.put(updateSingleStat(trans, item), expiryTime);
        }

        trans.commit();
    }


    final synchronized void cleanup(final DataModificationTransaction trans, long now) {
        for (Iterator<Entry<K, Long>> it = trackedItems.entrySet().iterator();it.hasNext();){
            Entry<K, Long> e = it.next();
            if (now > e.getValue()) {
                cleanupSingleStat(trans, e.getKey());
                it.remove();
            }
        }
    }

    protected abstract void cleanupSingleStat(DataModificationTransaction trans, K item);
    protected abstract K updateSingleStat(DataModificationTransaction trans, I item);
}
