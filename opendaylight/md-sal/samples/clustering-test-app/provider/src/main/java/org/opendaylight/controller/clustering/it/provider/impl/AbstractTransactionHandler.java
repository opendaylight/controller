/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.TransactionsParams;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTransactionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionHandler.class);

    static final int SECOND_AS_NANO = 1000000000;
    //2^20 as in the model
    static final int MAX_ITEM = 1048576;

    static final QName ID_INTS =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-ints").intern();
    static final QName ID =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id").intern();
    static final QName ITEM =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "item").intern();
    static final QName NUMBER =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "number").intern();

    public static final QName ID_INT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15", "id-int").intern();
    public static final YangInstanceIdentifier ID_INTS_YID = YangInstanceIdentifier.of(ID_INTS);
    public static final YangInstanceIdentifier ID_INT_YID = ID_INTS_YID.node(ID_INT).toOptimized();

    private static final long DEAD_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final long runtimeNanos;
    private final long delayNanos;

    @GuardedBy("this")
    private Queue<ListenableFuture<Void>> currentFutures;
    @GuardedBy("this")
    private List<ListenableFuture<Void>> failedFutures;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService executor;
    private Stopwatch stopwatch;
    private long txCounter;

    AbstractTransactionHandler(final TransactionsParams params) {
        runtimeNanos = TimeUnit.SECONDS.toNanos(params.getSeconds());
        delayNanos = SECOND_AS_NANO / params.getTransactionsPerSecond();
    }

    final synchronized void doStart() {
        executor = Executors.newSingleThreadScheduledExecutor();
        currentFutures = new ArrayDeque<>();
        failedFutures = new ArrayList<>();
        stopwatch = Stopwatch.createStarted();
        scheduledFuture = executor.scheduleAtFixedRate(this::execute, 0, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void execute() {
        final long txId = txCounter++;
        final ListenableFuture<Void> execFuture = execWrite(txId);

        synchronized (this) {
            currentFutures.add(execFuture);
        }

        final long elapsed = stopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsed < runtimeNanos) {
            // Not elapsed yet: add a cleaner callback and return
            Futures.addCallback(execFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    txSuccess(execFuture, txId);
                }

                @Override
                public void onFailure(final Throwable cause) {
                    txFailure(execFuture, txId, cause);
                }
            });
            return;
        }

        final List<ListenableFuture<Void>> futures;
        synchronized (this) {
            LOG.debug("Reached maximum run time with {} failed and {} outstanding futures, cleaning up",
                failedFutures.size(), currentFutures.size());
            scheduledFuture.cancel(false);
            executor.shutdown();

            futures = new ArrayList<>(failedFutures.size() + currentFutures.size());
            futures.addAll(failedFutures);
            futures.addAll(currentFutures);
        }

        LOG.debug("Waiting for {} futures to complete", futures.size());
        final ListenableFuture<List<Void>> allFutures = Futures.allAsList(futures);
        try {
            // Timeout from cds should be 2 minutes so leave some leeway.
            allFutures.get(125, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            LOG.error("Write transactions failed.", cause);
            runFailed(cause);
            return;
        } catch (InterruptedException | TimeoutException e) {
            runTimedOut(futures, e);
            return;
        }

        LOG.debug("All futures completed successfully.");
        runSuccessful(txCounter);
    }

    final synchronized void txSuccess(final ListenableFuture<Void> execFuture, final long txId) {
        LOG.debug("Future #{} completed successfully", txId);
        currentFutures.remove(execFuture);
    }

    final synchronized void txFailure(final ListenableFuture<Void> execFuture, final long txId, final Throwable cause) {
        LOG.debug("Future #{} failed", txId, cause);
        currentFutures.remove(execFuture);
        failedFutures.add(execFuture);
    }


    private void runTimedOut(final List<ListenableFuture<Void>> futures, final Exception e) {
        LOG.error("Write transactions failed.", e);

        for (int i = 0; i < futures.size(); i++) {
            final ListenableFuture<Void> future = futures.get(i);

            try {
                future.get(0, TimeUnit.NANOSECONDS);
            } catch (final TimeoutException fe) {
                LOG.warn("Future #{}/{} not completed yet", i, futures.size());
            } catch (final ExecutionException fe) {
                LOG.warn("Future #{}/{} failed", i, futures.size(), e.getCause());
            } catch (final InterruptedException fe) {
                LOG.warn("Interrupted while examining future #{}/{}", i, futures.size(), e);
            }
        }

        runTimedOut(e);
    }

    abstract ListenableFuture<Void> execWrite(final long txId);

    abstract void runFailed(Throwable cause);

    abstract void runSuccessful(long allTx);

    abstract void runTimedOut(Exception cause);
}
