/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataListener.class);
    private static final long MAX_ELAPSED_NANOS = TimeUnit.SECONDS.toNanos(4);

    private final Stopwatch ticksSinceLast = Stopwatch.createUnstarted();

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private NormalizedNode<?, ?> localCopy;

    AbstractDataListener() {
        // Do not allow instantiation from outside of the class
    }

    public final Optional<NormalizedNode<?, ?>> getLocalCopy() {
        return Optional.fromNullable(localCopy);
    }

    public final boolean checkEqual(final NormalizedNode<?, ?> expected) {
        checkState(localCopy != null, "No state has been captured");
        return localCopy.equals(expected);
    }

    public final Future<Void> tryFinishProcessing() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        final SettableFuture<Void> settableFuture = SettableFuture.create();

        scheduledFuture = executorService.scheduleAtFixedRate(() -> checkFinishedTask(settableFuture), 0, 1,
            TimeUnit.SECONDS);
        return settableFuture;
    }

    private void checkFinishedTask(final SettableFuture<Void> future) {
        if (ticksSinceLast.elapsed(TimeUnit.NANOSECONDS) > MAX_ELAPSED_NANOS) {
            scheduledFuture.cancel(false);
            future.set(null);

            executorService.shutdown();
        }
    }

    final void onReceivedChanges(@Nonnull final Collection<DataTreeCandidate> changes) {
        // do not log the change into debug, only use trace since it will lead to OOM on default heap settings
        LOG.debug("Received {} data tree changed events", changes.size());
        changes.forEach(this::processChange);
    }

    final void processChange(final DataTreeCandidate change) {
        LOG.trace("Processing change {}", change);

        final DataTreeCandidateNode root = change.getRootNode();
        final Optional<NormalizedNode<?, ?>> beforeOpt = root.getDataBefore();
        final Optional<NormalizedNode<?, ?>> afterOpt = root.getDataAfter();

        if (!afterOpt.isPresent()) {
            LOG.warn("getDataAfter() is missing from notification. change: {}", change);
            return;
        }

        final NormalizedNode<?, ?> after = afterOpt.get();
        LOG.trace("Received change, data before: {}, data after: {}", beforeOpt, after);

        if (localCopy != null) {
            if (!beforeOpt.isPresent()) {
                if (!checkEqual(beforeOpt.get())) {
                    LOG.warn("Ignoring notification.");
                    LOG.trace("Ignored notification content: {}", change);
                    return;
                }
            } else {
                LOG.warn("No before-state reported in {}, attempting to continue", change);
            }
        }

        localCopy = after;
    }

    final void onReceivedError(final Collection<? extends Throwable> errors) {
        final Iterator<? extends Throwable> it = errors.iterator();
        final Throwable first = it.next();
        it.forEachRemaining(first::addSuppressed);

        LOG.error("Listener failed", first);

        // FIXME: mark the failure
    }
}
