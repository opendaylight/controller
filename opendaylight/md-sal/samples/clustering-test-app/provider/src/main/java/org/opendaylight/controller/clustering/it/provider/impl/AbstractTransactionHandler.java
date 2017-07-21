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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.TransactionsParams;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTransactionHandler {
    private enum State {
        RUNNING,
        WAITING,
        SUCCESSFUL,
        FAILED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionHandler.class);

    static final int SECOND_AS_NANO = 1_000_000_000;
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

    private static final long DEAD_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(15);

    /*
     * writingExecutor is a single thread executor. Only this thread will write to datastore,
     * incurring sleep penalties if backend is not responsive. This thread never changes, but reads State.
     * This thread only adds to futures set.
     */
    private final ScheduledExecutorService writingExecutor = FinalizableScheduledExecutorService.newSingleThread();
    /*
     * completingExecutor is a single thread executor. Only this thread writes to State.
     * This thread should never incur any sleep penalty, so RPC response should always come on time.
     * This thread only removes from futures set.
     */
    private final ScheduledExecutorService completingExecutor = FinalizableScheduledExecutorService.newSingleThread();
    private final Collection<ListenableFuture<Void>> futures = Collections.synchronizedSet(new HashSet<>());
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private final long runtimeNanos;
    private final long delayNanos;

    private ScheduledFuture<?> writingFuture;
    private ScheduledFuture<?> completingFuture;
    private long txCounter;
    private volatile State state;

    AbstractTransactionHandler(final TransactionsParams params) {
        runtimeNanos = TimeUnit.SECONDS.toNanos(params.getSeconds());
        delayNanos = SECOND_AS_NANO / params.getTransactionsPerSecond();
    }

    final synchronized void doStart() {
        // Setup state first...
        stopwatch.start();
        state = State.RUNNING;

        writingFuture = writingExecutor.scheduleAtFixedRate(this::execute, 0, delayNanos, TimeUnit.NANOSECONDS);
    }

    private void execute() {
        // Single volatile access
        final State local = state;

        switch (local) {
            case FAILED:
                // This could happen due to scheduling artifacts
                break;
            case RUNNING:
                runningExecute();
                break;
            default:
                throw new IllegalStateException("Unhandled state " + local);
        }
    }

    private void runningExecute() {
        final long elapsed = stopwatch.elapsed(TimeUnit.NANOSECONDS);
        if (elapsed >= runtimeNanos) {
            LOG.debug("Reached maximum run time with {} outstanding futures", futures.size());
            completingExecutor.schedule(this::runtimeUp, 0, TimeUnit.SECONDS);
            return;
        }

        // Not completed yet: create a transaction and hook it up
        final long txId = txCounter++;
        final ListenableFuture<Void> execFuture = execWrite(txId);
        LOG.debug("New future #{} allocated", txId);

        // Ordering is important: we need to add the future before hooking the callback
        futures.add(execFuture);
        Futures.addCallback(execFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                txSuccess(execFuture, txId);
            }

            @Override
            public void onFailure(final Throwable cause) {
                txFailure(execFuture, txId, cause);
            }
        }, completingExecutor);
    }

    private void runtimeUp() {
        // checkSuccessful has two call sites, it is simpler to create completingFuture unconditionally.
        completingFuture = completingExecutor.schedule(this::checkComplete, DEAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!checkSuccessful()) {
            state = State.WAITING;
            writingFuture.cancel(false);
        }
    }

    private boolean checkSuccessful() {
        if (futures.isEmpty()) {
            LOG.debug("Completed waiting for all futures");
            state = State.SUCCESSFUL;
            completingFuture.cancel(false);
            runSuccessful(txCounter);
            return true;
        }

        return false;
    }

    final void txSuccess(final ListenableFuture<Void> execFuture, final long txId) {
        LOG.debug("Future #{} completed successfully", txId);
        futures.remove(execFuture);

        final State local = state;
        switch (local) {
            case FAILED:
            case RUNNING:
                // No-op
                break;
            case WAITING:
                checkSuccessful();
                break;
            default:
                throw new IllegalStateException("Unhandled state " + local);
        }
    }

    final void txFailure(final ListenableFuture<Void> execFuture, final long txId, final Throwable cause) {
        LOG.debug("Future #{} failed", txId, cause);
        futures.remove(execFuture);

        final State local = state;
        switch (local) {
            case FAILED:
                // no-op
                break;
            case RUNNING:
            case WAITING:
                state = State.FAILED;
                writingFuture.cancel(false);
                runFailed(cause);
                break;
            default:
                throw new IllegalStateException("Unhandled state " + local);
        }
    }

    private void checkComplete() {
        final int size = futures.size();
        if (size == 0) {
            return;
        }

        // Guards iteration against concurrent modification from callbacks
        synchronized (futures) {
            int offset = 0;

            for (ListenableFuture<Void> future : futures) {
                try {
                    future.get(0, TimeUnit.NANOSECONDS);
                } catch (final TimeoutException e) {
                    LOG.warn("Future #{}/{} not completed yet", offset, size);
                } catch (final ExecutionException e) {
                    LOG.warn("Future #{}/{} failed", offset, size, e.getCause());
                } catch (final InterruptedException e) {
                    LOG.warn("Interrupted while examining future #{}/{}", offset, size, e);
                }

                ++offset;
            }
        }

        state = State.FAILED;
        runTimedOut(new TimeoutException("Collection did not finish in " + DEAD_TIMEOUT_SECONDS + " seconds"));
    }

    abstract ListenableFuture<Void> execWrite(final long txId);

    abstract void runFailed(Throwable cause);

    abstract void runSuccessful(long allTx);

    abstract void runTimedOut(Exception cause);
}
