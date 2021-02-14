/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(forRemoval = true)
public class IdIntsDOMDataTreeLIstener implements DOMDataTreeListener {

    private static final Logger LOG = LoggerFactory.getLogger(IdIntsDOMDataTreeLIstener.class);
    private static final long SECOND_AS_NANO = 1000000000;

    private NormalizedNode<?, ?> localCopy = null;
    private final AtomicLong lastNotifTimestamp = new AtomicLong(0);
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService executorService;

    @Override
    public void onDataTreeChanged(final Collection<DataTreeCandidate> changes,
                                  final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> subtrees) {

        // There should only be one candidate reported
        Preconditions.checkState(changes.size() == 1);

        lastNotifTimestamp.set(System.nanoTime());

        // do not log the change into debug, only use trace since it will lead to OOM on default heap settings
        LOG.debug("Received data tree changed");

        changes.forEach(change -> {
            if (change.getRootNode().getDataAfter().isPresent()) {
                LOG.trace("Received change, data before: {}, data after: {}",
                        change.getRootNode().getDataBefore().isPresent()
                                ? change.getRootNode().getDataBefore().get() : "",
                        change.getRootNode().getDataAfter().get());

                if (localCopy == null || checkEqual(change.getRootNode().getDataBefore().get())) {
                    localCopy = change.getRootNode().getDataAfter().get();
                } else {
                    LOG.warn("Ignoring notification.");
                    LOG.trace("Ignored notification content: {}", change);
                }
            } else {
                LOG.warn("getDataAfter() is missing from notification. change: {}", change);
            }
        });
    }

    @Override
    public void onDataTreeFailed(final Collection<DOMDataTreeListeningException> causes) {

    }

    public boolean hasTriggered() {
        return localCopy != null;
    }

    public Future<Void> tryFinishProcessing() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        final SettableFuture<Void> settableFuture = SettableFuture.create();

        scheduledFuture = executorService.scheduleAtFixedRate(new CheckFinishedTask(settableFuture),
                0, 1, TimeUnit.SECONDS);
        return settableFuture;
    }

    public boolean checkEqual(final NormalizedNode<?, ?> expected) {
        return localCopy.equals(expected);
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
