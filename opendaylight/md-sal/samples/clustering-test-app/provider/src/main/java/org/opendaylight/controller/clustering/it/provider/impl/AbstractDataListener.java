/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataListener.class);
    private static final long MAX_ELAPSED_NANOS = TimeUnit.SECONDS.toNanos(4);

    @GuardedBy("ticksSinceLast")
    private final Stopwatch ticksSinceLast = Stopwatch.createUnstarted();

    private DataListenerState state = DataListenerState.initial();
    private ScheduledFuture<?> scheduledFuture;

    AbstractDataListener() {
        // Do not allow instantiation from outside of the class
    }

    public final ListenableFuture<DataListenerState> tryFinishProcessing(final ListenerRegistration<?> ddtlReg) {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        final SettableFuture<DataListenerState> future = SettableFuture.create();

        scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            final long elapsed;
            synchronized (ticksSinceLast) {
                elapsed = ticksSinceLast.elapsed(TimeUnit.NANOSECONDS);
            }

            if (elapsed > MAX_ELAPSED_NANOS) {
                ddtlReg.close();
                future.set(state);
                scheduledFuture.cancel(false);
                executorService.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
        return future;
    }

    final void onReceivedChanges(@Nonnull final Collection<DataTreeCandidate> changes) {
        // do not log the change into debug, only use trace since it will lead to OOM on default heap settings
        LOG.debug("Received {} data tree changed events", changes.size());
        changes.forEach(change -> {
            LOG.trace("Processing change {}", change);
            state = state.append(change);
        });

        synchronized (this) {
            ticksSinceLast.reset().start();
        }
    }

    final void onReceivedError(final Collection<? extends Throwable> errors) {
        final Iterator<? extends Throwable> it = errors.iterator();
        final Throwable first = it.next();
        it.forEachRemaining(first::addSuppressed);

        LOG.error("Listener failed", first);

        // FIXME: mark the failure
    }
}
