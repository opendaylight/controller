/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import static com.google.common.base.Preconditions.checkState;
import static org.opendaylight.controller.clustering.it.provider.impl.AbstractTransactionHandler.ITEM;

import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdIntsListener implements ClusteredDOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(IdIntsListener.class);
    private static final long SECOND_AS_NANO = 1000000000;

    private volatile NormalizedNode localCopy;
    private final AtomicLong lastNotifTimestamp = new AtomicLong(0);
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;

    @Override
    public void onInitialData() {
        // Intentional no-op
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {

        // There should only be one candidate reported
        checkState(changes.size() == 1);

        lastNotifTimestamp.set(System.nanoTime());

        // do not log the change into debug, only use trace since it will lead to OOM on default heap settings
        LOG.debug("Received data tree changed");

        changes.forEach(change -> {
            if (change.getRootNode().getDataAfter().isPresent()) {
                LOG.trace("Received change, data before: {}, data after: {}",
                        change.getRootNode().getDataBefore().isPresent()
                                ? change.getRootNode().getDataBefore().get() : "",
                        change.getRootNode().getDataAfter().get());

                localCopy = change.getRootNode().getDataAfter().get();
            } else {
                LOG.warn("getDataAfter() is missing from notification. change: {}", change);
            }
        });
    }

    public boolean hasTriggered() {
        return localCopy != null;
    }

    public boolean checkEqual(final NormalizedNode expected) {
        return localCopy.equals(expected);
    }

    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public String diffWithLocalCopy(final NormalizedNode expected) {
        return diffNodes((MapNode)expected, (MapNode)localCopy);
    }

    public Future<Void> tryFinishProcessing() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        final SettableFuture<Void> settableFuture = SettableFuture.create();

        scheduledFuture = executorService.scheduleAtFixedRate(new CheckFinishedTask(settableFuture),
                0, 1, TimeUnit.SECONDS);
        return settableFuture;
    }

    public static String diffNodes(final MapNode expected, final MapNode actual) {
        StringBuilder builder = new StringBuilder("MapNodes diff:");

        final YangInstanceIdentifier.NodeIdentifier itemNodeId = new YangInstanceIdentifier.NodeIdentifier(ITEM);

        Map<NodeIdentifierWithPredicates, MapEntryNode> expIdIntMap = new HashMap<>();
        expected.body().forEach(node -> expIdIntMap.put(node.getIdentifier(), node));

        actual.body().forEach(actIdInt -> {
            final MapEntryNode expIdInt = expIdIntMap.remove(actIdInt.getIdentifier());
            if (expIdInt == null) {
                builder.append('\n').append("  Unexpected id-int entry for ").append(actIdInt.getIdentifier());
                return;
            }

            Map<NodeIdentifierWithPredicates, MapEntryNode> expItemMap = new HashMap<>();
            ((MapNode)expIdInt.findChildByArg(itemNodeId).get()).body()
                .forEach(node -> expItemMap.put(node.getIdentifier(), node));

            ((MapNode)actIdInt.findChildByArg(itemNodeId).get()).body().forEach(actItem -> {
                final MapEntryNode expItem = expItemMap.remove(actItem.getIdentifier());
                if (expItem == null) {
                    builder.append('\n').append("  Unexpected item entry ").append(actItem.getIdentifier())
                        .append(" for id-int entry ").append(actIdInt.getIdentifier());
                }
            });

            expItemMap.values().forEach(node -> builder.append('\n')
                .append("  Actual is missing item entry ").append(node.getIdentifier())
                    .append(" for id-int entry ").append(actIdInt.getIdentifier()));
        });

        expIdIntMap.values().forEach(node -> builder.append('\n')
            .append("  Actual is missing id-int entry for ").append(node.getIdentifier()));

        return builder.toString();
    }

    private class CheckFinishedTask implements Runnable {

        private final SettableFuture<Void> future;

        CheckFinishedTask(final SettableFuture<Void> future) {
            this.future = future;
        }

        @Override
        public void run() {
            if (System.nanoTime() - lastNotifTimestamp.get() > SECOND_AS_NANO * 4) {
                scheduledFuture.cancel(false);
                future.set(null);

                executorService.shutdown();
            }
        }
    }
}
