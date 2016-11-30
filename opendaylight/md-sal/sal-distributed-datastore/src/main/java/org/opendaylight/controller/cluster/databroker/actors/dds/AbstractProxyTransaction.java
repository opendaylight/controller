/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class translating transaction operations towards a particular backend shard.
 *
 * <p>
 * This class is not safe to access from multiple application threads, as is usual for transactions. Internal state
 * transitions coming from interactions with backend are expected to be thread-safe.
 *
 * <p>
 * This class interacts with the queueing mechanism in ClientActorBehavior, hence once we arrive at a decision
 * to use either a local or remote implementation, we are stuck with it. We can re-evaluate on the next transaction.
 *
 * @author Robert Varga
 */
abstract class AbstractProxyTransaction implements Identifiable<TransactionIdentifier> {
    /**
     * Marker object used instead of read-type of requests, which are satisfied only once. This has a lower footprint
     * and allows compressing multiple requests into a single entry.
     */
    @NotThreadSafe
    private static final class IncrementSequence {
        private long delta = 1;

        long getDelta() {
            return delta;
        }

        void incrementDelta() {
            delta++;
        }
    }

    private static enum SealState {
        /**
         * The user has not sealed the transaction yet.
         */
        OPEN,
        /**
         * The user has sealed the transaction, but has not issued a canCommit()
         */
        SEALED,
        /**
         * The user has sealed the transaction and has issued a canCommit()
         */
        FLUSHED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractProxyTransaction.class);

    private final Deque<Object> successfulRequests = new ArrayDeque<>();
    private final ProxyHistory parent;

    /*
     * Atomic state-keeping is required to synchronize the process of propagating completed transaction state towards
     * the backend -- which may include a successor.
     *
     * Successor, unlike {@link AbstractProxyTransaction#seal()} is triggered from the client actor thread, which means
     * the successor placement needs to be atomic with regard to the application thread.
     *
     * In the common case, the application thread performs performs the seal operations and then "immediately" sends
     * the corresponding message. The uncommon case is when the seal and send operations race with a connect completion
     * or timeout, when a successor is injected.
     *
     * This leaves the problem of needing to completely transferring state just after all queued messages are replayed
     * after a successor was injected, so that it can be properly sealed if we are racing.
     */
    private volatile SealState sealed = SealState.OPEN;
    @GuardedBy("this")
    private AbstractProxyTransaction successor;
    @GuardedBy("this")
    private CountDownLatch successorLatch;

    // Accessed from user thread only, which may not access this object concurrently
    private long sequence;


    AbstractProxyTransaction(final ProxyHistory parent) {
        this.parent = Preconditions.checkNotNull(parent);
    }

    final ActorRef localActor() {
        return parent.localActor();
    }

    private void incrementSequence(final long delta) {
        sequence += delta;
        LOG.debug("Transaction {} incremented sequence to {}", this, sequence);
    }

    final long nextSequence() {
        final long ret = sequence++;
        LOG.debug("Transaction {} allocated sequence {}", this, ret);
        return ret;
    }

    final void delete(final YangInstanceIdentifier path) {
        checkNotSealed();
        doDelete(path);
    }

    final void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotSealed();
        doMerge(path, data);
    }

    final void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotSealed();
        doWrite(path, data);
    }

    final CheckedFuture<Boolean, ReadFailedException> exists(final YangInstanceIdentifier path) {
        checkNotSealed();
        return doExists(path);
    }

    final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final YangInstanceIdentifier path) {
        checkNotSealed();
        return doRead(path);
    }

    final void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        LOG.debug("Transaction proxy {} sending request {} callback {}", this, request, callback);
        parent.sendRequest(request, callback);
    }

    /**
     * Seal this transaction before it is either committed or aborted.
     */
    final void seal() {
        final CountDownLatch localLatch;

        synchronized (this) {
            checkNotSealed();
            doSeal();

            // Fast path: no successor
            if (successor == null) {
                sealed = SealState.SEALED;
                parent.onTransactionSealed(this);
                return;
            }

            localLatch = successorLatch;
        }

        // Slow path: wait for the latch
        LOG.debug("{} waiting on successor latch", getIdentifier());
        try {
            localLatch.await();
        } catch (InterruptedException e) {
            LOG.warn("{} interrupted while waiting for latch", getIdentifier());
            throw Throwables.propagate(e);
        }

        synchronized (this) {
            LOG.debug("{} reacquired lock", getIdentifier());

            flushState(successor);
            successor.seal();

            sealed = SealState.FLUSHED;
            parent.onTransactionSealed(this);
        }
    }

    private void checkNotSealed() {
        Preconditions.checkState(sealed == SealState.OPEN, "Transaction %s has already been sealed", getIdentifier());
    }

    private SealState checkSealed() {
        final SealState local = sealed;
        Preconditions.checkState(local != SealState.OPEN, "Transaction %s has not been sealed yet", getIdentifier());
        return local;
    }

    final void recordSuccessfulRequest(final @Nonnull TransactionRequest<?> req) {
        successfulRequests.add(Verify.verifyNotNull(req));
    }

    final void recordFinishedRequest() {
        final Object last = successfulRequests.peekLast();
        if (last instanceof IncrementSequence) {
            ((IncrementSequence) last).incrementDelta();
        } else {
            successfulRequests.addLast(new IncrementSequence());
        }
    }

    /**
     * Abort this transaction. This is invoked only for read-only transactions and will result in an explicit message
     * being sent to the backend.
     */
    final void abort() {
        checkNotSealed();
        doAbort();
        parent.abortTransaction(this);
    }

    final void abort(final VotingFuture<Void> ret) {
        checkSealed();

        sendAbort(t -> {
            if (t instanceof TransactionAbortSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }

            // This is a terminal request, hence we do not need to record it
            LOG.debug("Transaction {} abort completed", this);
            parent.completeTransaction(this);
        });
    }

    final void sendAbort(final Consumer<Response<?, ?>> callback) {
        sendRequest(new TransactionAbortRequest(getIdentifier(), nextSequence(), localActor()), callback);
    }

    /**
     * Commit this transaction, possibly in a coordinated fashion.
     *
     * @param coordinated True if this transaction should be coordinated across multiple participants.
     * @return Future completion
     */
    final ListenableFuture<Boolean> directCommit() {
        final CountDownLatch localLatch;

        synchronized (this) {
            final SealState local = checkSealed();

            // Fast path: no successor asserted
            if (successor == null) {
                Verify.verify(local == SealState.SEALED);

                final SettableFuture<Boolean> ret = SettableFuture.create();
                sendRequest(Verify.verifyNotNull(commitRequest(false)), t -> {
                    if (t instanceof TransactionCommitSuccess) {
                        ret.set(Boolean.TRUE);
                    } else if (t instanceof RequestFailure) {
                        ret.setException(((RequestFailure<?, ?>) t).getCause());
                    } else {
                        ret.setException(new IllegalStateException("Unhandled response " + t.getClass()));
                    }

                    // This is a terminal request, hence we do not need to record it
                    LOG.debug("Transaction {} directCommit completed", this);
                    parent.completeTransaction(this);
                });

                sealed = SealState.FLUSHED;
                return ret;
            }

            // We have a successor, take its latch
            localLatch = successorLatch;
        }

        // Slow path: we need to wait for the successor to completely propagate
        LOG.debug("{} waiting on successor latch", getIdentifier());
        try {
            localLatch.await();
        } catch (InterruptedException e) {
            LOG.warn("{} interrupted while waiting for latch", getIdentifier());
            throw Throwables.propagate(e);
        }

        synchronized (this) {
            LOG.debug("{} reacquired lock", getIdentifier());

            final SealState local = sealed;
            Verify.verify(local == SealState.FLUSHED);

            return successor.directCommit();
        }
    }

    final void canCommit(final VotingFuture<?> ret) {
        final CountDownLatch localLatch;

        synchronized (this) {
            final SealState local = checkSealed();

            // Fast path: no successor asserted
            if (successor == null) {
                Verify.verify(local == SealState.SEALED);

                final TransactionRequest<?> req = Verify.verifyNotNull(commitRequest(true));
                sendRequest(req, t -> {
                    if (t instanceof TransactionCanCommitSuccess) {
                        ret.voteYes();
                    } else if (t instanceof RequestFailure) {
                        ret.voteNo(((RequestFailure<?, ?>) t).getCause());
                    } else {
                        ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
                    }

                    recordSuccessfulRequest(req);
                    LOG.debug("Transaction {} canCommit completed", this);
                });

                sealed = SealState.FLUSHED;
                return;
            }

            // We have a successor, take its latch
            localLatch = successorLatch;
        }

        // Slow path: we need to wait for the successor to completely propagate
        LOG.debug("{} waiting on successor latch", getIdentifier());
        try {
            localLatch.await();
        } catch (InterruptedException e) {
            LOG.warn("{} interrupted while waiting for latch", getIdentifier());
            throw Throwables.propagate(e);
        }

        synchronized (this) {
            LOG.debug("{} reacquired lock", getIdentifier());

            final SealState local = sealed;
            Verify.verify(local == SealState.FLUSHED);

            successor.canCommit(ret);
        }
    }

    final void preCommit(final VotingFuture<?> ret) {
        checkSealed();

        final TransactionRequest<?> req = new TransactionPreCommitRequest(getIdentifier(), nextSequence(),
            localActor());
        sendRequest(req, t -> {
            if (t instanceof TransactionPreCommitSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }

            recordSuccessfulRequest(req);
            LOG.debug("Transaction {} preCommit completed", this);
        });
    }

    void doCommit(final VotingFuture<?> ret) {
        checkSealed();

        sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), t -> {
            if (t instanceof TransactionCommitSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause());
            } else {
                ret.voteNo(new IllegalStateException("Unhandled response " + t.getClass()));
            }

            LOG.debug("Transaction {} doCommit completed", this);
            parent.completeTransaction(this);
        });
    }

    final synchronized void startReconnect(final AbstractProxyTransaction successor) {
        Preconditions.checkState(this.successor == null);
        this.successor = Preconditions.checkNotNull(successor);

        for (Object obj : successfulRequests) {
            if (obj instanceof TransactionRequest) {
                LOG.debug("Forwarding request {} to successor {}", obj, successor);
                successor.handleForwardedRemoteRequest((TransactionRequest<?>) obj, null);
            } else {
                Verify.verify(obj instanceof IncrementSequence);
                successor.incrementSequence(((IncrementSequence) obj).getDelta());
            }
        }
        LOG.debug("{} replayed {} successful requests", getIdentifier(), successfulRequests.size());
        successfulRequests.clear();

        /*
         * Before releasing the lock we need to make sure that a call to seal() blocks until we have completed
         * finishConnect().
         */
        successorLatch = new CountDownLatch(1);
    }

    final synchronized void finishReconnect() {
        Preconditions.checkState(successorLatch != null);

        if (sealed == SealState.SEALED) {
            /*
             * If this proxy is in the 'sealed, have not sent canCommit' state. If so, we need to forward current
             * leftover state to the successor now.
             */
            flushState(successor);
            successor.seal();
            sealed = SealState.FLUSHED;
        }

        // All done, release the latch, unblocking seal() and canCommit()
        successorLatch.countDown();
    }

    /**
     * Invoked from a retired connection for requests which have been in-flight and need to be re-adjusted
     * and forwarded to the successor connection.
     *
     * @param request Request to be forwarded
     * @param callback Original callback
     * @throws RequestException when the request is unhandled by the successor
     */
    final synchronized void replayRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        Preconditions.checkState(successor != null, "%s does not have a successor set", this);

        if (successor instanceof LocalProxyTransaction) {
            forwardToLocal((LocalProxyTransaction)successor, request, callback);
        } else if (successor instanceof RemoteProxyTransaction) {
            forwardToRemote((RemoteProxyTransaction)successor, request, callback);
        } else {
            throw new IllegalStateException("Unhandled successor " + successor);
        }
    }

    abstract void doDelete(final YangInstanceIdentifier path);

    abstract void doMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data);

    abstract void doWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data);

    abstract CheckedFuture<Boolean, ReadFailedException> doExists(final YangInstanceIdentifier path);

    abstract CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> doRead(
            final YangInstanceIdentifier path);

    abstract void doSeal();

    abstract void doAbort();

    @GuardedBy("this")
    abstract void flushState(AbstractProxyTransaction successor);

    abstract TransactionRequest<?> commitRequest(boolean coordinated);

    /**
     * Invoked from {@link RemoteProxyTransaction} when it replays its successful requests to its successor. There is
     * no equivalent of this call from {@link LocalProxyTransaction} because it does not send a request until all
     * operations are packaged in the message.
     *
     * <p>
     * Note: this method is invoked by the predecessor on the successor.
     *
     * @param request Request which needs to be forwarded
     * @param callback Callback to be invoked once the request completes
     */
    abstract void handleForwardedRemoteRequest(TransactionRequest<?> request,
            @Nullable Consumer<Response<?, ?>> callback);

    /**
     * Replay a request originating in this proxy to a successor remote proxy.
     */
    abstract void forwardToRemote(RemoteProxyTransaction successor, TransactionRequest<?> request,
            Consumer<Response<?, ?>> callback);

    /**
     * Replay a request originating in this proxy to a successor local proxy.
     */
    abstract void forwardToLocal(LocalProxyTransaction successor, TransactionRequest<?> request,
            Consumer<Response<?, ?>> callback);
}
