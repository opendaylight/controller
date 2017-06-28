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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private abstract static class Phase {
        abstract void txSuccess(ListenableFuture<Void> execFuture, long txId);

        abstract void txFailure(ListenableFuture<Void> execFuture, long txId, Throwable cause);
    }

    private static final class Running extends Phase {
        private final Queue<ListenableFuture<Void>> futures = new ArrayDeque<>();
        private Throwable failure;

        void addFuture(final ListenableFuture<Void> execFuture) {
            futures.add(execFuture);
        }

        @Override
        void txSuccess(final ListenableFuture<Void> execFuture, final long txId) {
            futures.remove(execFuture);
        }

        @Override
        void txFailure(final ListenableFuture<Void> execFuture, final long txId, final Throwable cause) {
            futures.remove(execFuture);
            if (failure != null) {
                failure = cause;
            }
        }

        Optional<Throwable> getFailure() {
            return Optional.ofNullable(failure);
        }
    }

    private final class Collecting extends Phase {
        private final List<ListenableFuture<Void>> futures;
        private boolean done;

        Collecting(final Collection<ListenableFuture<Void>> futures) {
            this.futures = new ArrayList<>(futures);
        }

        @Override
        void txSuccess(final ListenableFuture<Void> execFuture, final long txId) {
            futures.remove(execFuture);
            if (futures.isEmpty() && !done) {
                LOG.debug("All futures completed successfully.");
                runSuccessful(txCounter);
            }
        }

        @Override
        void txFailure(final ListenableFuture<Void> execFuture, final long txId, final Throwable cause) {
            futures.remove(execFuture);
            done = true;
            runFailed(cause);
        }
    }

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

    static final long INIT_TX_TIMEOUT_SECONDS = 125;

    private static final long TRANSACTIONS_COMPLETED_TIMEOUT_SECONDS = 125 + 18;  // ROBOT only waits 20 seconds after isolation and 125.

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Stopwatch stopwatch = Stopwatch.createStarted();
    private final long runtimeNanos;
    private final long delayNanos;

    private ScheduledFuture<?> scheduledFuture;
    private long txCounter;
    @GuardedBy("this")
    private Phase phase;

    AbstractTransactionHandler(final TransactionsParams params) {
        runtimeNanos = TimeUnit.SECONDS.toNanos(params.getSeconds());
        delayNanos = SECOND_AS_NANO / params.getTransactionsPerSecond();
    }

    final synchronized void doStart() {
        phase = new Running();
        scheduledFuture = executor.scheduleAtFixedRate(this::execute, 0, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void execute() {
        final long elapsed = stopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsed < runtimeNanos) {
            LOG.debug("Elapsed {} nanos, next transaction is due.", elapsed);
            // Not completed yet: create a transaction and hook it up
            final long txId = txCounter++;
            final ListenableFuture<Void> execFuture = execWrite(txId);

            // Ordering is important: we need to add the future before hooking the callback
            synchronized (this) {
                ((Running) phase).addFuture(execFuture);
            }
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
        } else {
            LOG.debug("Elapsed {} nanos, starting collection phase.", elapsed);
            startCollection();
        }
    }

    private synchronized void startCollection() {
        scheduledFuture.cancel(false);

        final Running running = (Running) phase;
        final Optional<Throwable> failure = running.getFailure();
        if (failure.isPresent()) {
            executor.shutdown();
            runFailed(failure.get());
            return;
        }

        LOG.debug("Reached maximum run time with {} outstanding futures", running.futures.size());
        if (running.futures.isEmpty()) {
            executor.shutdown();
            runSuccessful(txCounter);
            return;
        }

        phase = new Collecting(running.futures);
        executor.schedule(this::checkCollection, TRANSACTIONS_COMPLETED_TIMEOUT_SECONS, TimeUnit.SECONDS);
        LOG.debug("Telling executor to shutdown itself after performing checkCollection.");
        executor.shutdown();
    }

    final synchronized void txSuccess(final ListenableFuture<Void> execFuture, final long txId) {
        LOG.debug("Future #{} completed successfully", txId);
        phase.txSuccess(execFuture, txId);
    }

    final synchronized void txFailure(final ListenableFuture<Void> execFuture, final long txId, final Throwable cause) {
        LOG.debug("Future #{} failed", txId, cause);
        phase.txFailure(execFuture, txId, cause);
    }

    private synchronized void checkCollection() {
        LOG.debug("Executing checkCollection.");
        final Collecting collecting = (Collecting) phase;
        if (collecting.done) {
            LOG.debug("Returning as collecting.txFailure should call runFailed");
            return;
        }
        if (collecting.futures.isEmpty()) {
            LOG.debug("Returning as collecting.txSuccess should call runSuccessful");
            return;
        }

        final int size = collecting.futures.size();
        for (int i = 0; i < size; i++) {
            final ListenableFuture<Void> future = collecting.futures.get(i);

            try {
                future.get(0, TimeUnit.NANOSECONDS);
            } catch (final TimeoutException e) {
                LOG.warn("Future #{}/{} not completed yet", i, size);
            } catch (final ExecutionException e) {
                LOG.warn("Future #{}/{} failed", i, size, e.getCause());
            } catch (final InterruptedException e) {
                LOG.warn("Interrupted while examining future #{}/{}", i, size, e);
            }
        }

        runTimedOut(new TimeoutException("Collection did not finish in " + TRANSACTIONS_COMPLETED_TIMEOUT_SECONS + " seconds"));
    }

    abstract ListenableFuture<Void> execWrite(final long txId);

    abstract void runFailed(Throwable cause);

    abstract void runSuccessful(long allTx);

    abstract void runTimedOut(Exception cause);
}
