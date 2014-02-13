/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import java.util.Collection;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

/**
 * There is a single instance of this class and that instance is responsible for
 * monitoring the operational data store for nodes being created/deleted and
 * notifying StatisticsProvider. These events then control the lifecycle of
 * NodeStatisticsHandler for a particular switch.
 */
final class FlowCapableTracker implements DataChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(FlowCapableTracker.class);

    private final InstanceIdentifier<FlowCapableNode> root;
    private final StatisticsProvider stats;

    private final Predicate<InstanceIdentifier<?>> filterIdentifiers = new Predicate<InstanceIdentifier<?>>() {
        @Override
        public boolean apply(final InstanceIdentifier<?> input) {
            /*
             * This notification has been triggered either by the ancestor,
             * descendant or directly for the FlowCapableNode itself. We
             * are not interested descendants, so let's prune them based
             * on the depth of their identifier.
             */
            if (root.getPath().size() < input.getPath().size()) {
                logger.debug("Ignoring notification for descendant {}", input);
                return false;
            }

            logger.debug("Including notification for {}", input);
            return true;
        }
    };

    public FlowCapableTracker(final StatisticsProvider stats, InstanceIdentifier<FlowCapableNode> root) {
        this.stats = Preconditions.checkNotNull(stats);
        this.root = Preconditions.checkNotNull(root);
    }

    /*
     * This method is synchronized because we want to make sure to serialize input
     * from the datastore. Competing add/remove could be problematic otherwise.
     */
    @Override
    public synchronized void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        logger.debug("Tracker at root {} processing notification", root);

        /*
         * First process all the identifiers which were removed, trying to figure out
         * whether they constitute removal of FlowCapableNode.
         */
        final Collection<NodeKey> removedNodes =
            Collections2.filter(Collections2.transform(
                Sets.filter(change.getRemovedOperationalData(), filterIdentifiers),
                new Function<InstanceIdentifier<?>, NodeKey>() {
                    @Override
                    public NodeKey apply(final InstanceIdentifier<?> input) {
                        final NodeKey key = input.firstKeyOf(Node.class, NodeKey.class);
                        if (key == null) {
                            // FIXME: do we have a backup plan?
                            logger.info("Failed to extract node key from {}", input);
                        }
                        return key;
                    }
                }), Predicates.notNull());
        stats.stopNodeHandlers(removedNodes);

        final Collection<NodeKey> addedNodes =
            Collections2.filter(Collections2.transform(
                Sets.filter(change.getCreatedOperationalData().keySet(), filterIdentifiers),
                new Function<InstanceIdentifier<?>, NodeKey>() {
                    @Override
                    public NodeKey apply(final InstanceIdentifier<?> input) {
                        final NodeKey key = input.firstKeyOf(Node.class, NodeKey.class);
                        if (key == null) {
                            // FIXME: do we have a backup plan?
                            logger.info("Failed to extract node key from {}", input);
                    }
                    return key;
                }
            }), Predicates.notNull());
        stats.startNodeHandlers(addedNodes);

        logger.debug("Tracker at root {} finished processing notification", root);
    }
}
