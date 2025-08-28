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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IdIntsListener implements DOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(IdIntsListener.class);
    private static final long SECOND_AS_NANO = 1000000000;

    private final AtomicLong lastNotifTimestamp = new AtomicLong(0);
    private ScheduledExecutorService executorService = null;
    private ScheduledFuture<?> scheduledFuture = null;

    private volatile NormalizedNode localCopy;

    @Override
    public void onInitialData() {
        // Intentional no-op
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeCandidate> changes) {

        // There should only be one candidate reported
        checkState(changes.size() == 1);

        lastNotifTimestamp.set(System.nanoTime());

        // do not log the change into debug, only use trace since it will lead to OOM on default heap settings
        LOG.debug("Received data tree changed");

        changes.forEach(change -> {
            final var root = change.getRootNode();
            final var after = root.dataAfter();
            if (after != null) {
                final var before = root.dataBefore();
                LOG.trace("Received change, data before: {}, data after: {}", before != null ? before : "", after);
                localCopy = after;
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

    public String diffWithLocalCopy(final NormalizedNode expected) {
        return diffNodes((MapNode)expected, (MapNode)localCopy);
    }

    public Future<Void> tryFinishProcessing() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        final var settableFuture = SettableFuture.<Void>create();

        scheduledFuture = executorService.scheduleAtFixedRate(new CheckFinishedTask(settableFuture),
                0, 1, TimeUnit.SECONDS);
        return settableFuture;
    }

    public static String diffNodes(final MapNode expected, final MapNode actual) {
        StringBuilder builder = new StringBuilder("MapNodes diff:");

        final var itemNodeId = new NodeIdentifier(ITEM);

        final var expIdIntMap = new HashMap<NodeIdentifierWithPredicates, MapEntryNode>();
        expected.body().forEach(node -> expIdIntMap.put(node.name(), node));

        actual.body().forEach(actIdInt -> {
            final var expIdInt = expIdIntMap.remove(actIdInt.name());
            if (expIdInt == null) {
                builder.append('\n').append("  Unexpected id-int entry for ").append(actIdInt.name());
                return;
            }

            final var expItemMap = new HashMap<NodeIdentifierWithPredicates, MapEntryNode>();
            ((MapNode)expIdInt.getChildByArg(itemNodeId)).body()
                .forEach(node -> expItemMap.put(node.name(), node));

            ((MapNode)actIdInt.getChildByArg(itemNodeId)).body().forEach(actItem -> {
                final var expItem = expItemMap.remove(actItem.name());
                if (expItem == null) {
                    builder.append('\n').append("  Unexpected item entry ").append(actItem.name())
                        .append(" for id-int entry ").append(actIdInt.name());
                }
            });

            expItemMap.values().forEach(node -> builder.append('\n')
                .append("  Actual is missing item entry ").append(node.name())
                    .append(" for id-int entry ").append(actIdInt.name()));
        });

        expIdIntMap.values().forEach(node -> builder.append('\n')
            .append("  Actual is missing id-int entry for ").append(node.name()));

        return builder.toString();
    }

    private final class CheckFinishedTask implements Runnable {
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
