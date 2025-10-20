/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.commands.AbstractLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ClosedTransactionException;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class translating transaction operations towards a particular backend shard.
 *
 * <p>This class is not safe to access from multiple application threads, as is usual for transactions. Internal state
 * transitions coming from interactions with backend are expected to be thread-safe.
 *
 * <p>This class interacts with the queueing mechanism in ClientActorBehavior, hence once we arrive at a decision to use
 * either a local or remote implementation, we are stuck with it. We can re-evaluate on the next transaction.
 */
abstract sealed class AbstractProxyTransaction implements Identifiable<TransactionIdentifier>
        permits LocalProxyTransaction, RemoteProxyTransaction {
    /**
     * Marker object used instead of read-type of requests, which are satisfied only once. This has a lower footprint
     * and allows compressing multiple requests into a single entry. This class is not thread-safe.
     */
    private static final class IncrementSequence {
        private final long sequence;
        private long delta = 0;

        IncrementSequence(final long sequence) {
            this.sequence = sequence;
        }

        long getDelta() {
            return delta;
        }

        long getSequence() {
            return sequence;
        }

        void incrementDelta() {
            delta++;
        }
    }

    /**
     * Base class for representing logical state of this proxy. See individual instantiations and {@link SuccessorState}
     * for details.
     */
    private static class State {
        private final String string;

        State(final String string) {
            this.string = requireNonNull(string);
        }

        @Override
        public final String toString() {
            return string;
        }
    }

    /**
     * State class used when a successor has interfered. Contains coordinator latch, the successor and previous state.
     * This is a temporary state introduced during reconnection process and is necessary for correct state hand-off
     * between the old connection (potentially being accessed by the user) and the new connection (being cleaned up
     * by the actor.
     *
     * <p>When a user operation encounters this state, it synchronizes on the it and wait until reconnection completes,
     * at which point the request is routed to the successor transaction. This is a relatively heavy-weight solution
     * to the problem of state transfer, but the user will observe it only if the race condition is hit.
     */
    private static class SuccessorState extends State {
        private final CountDownLatch latch = new CountDownLatch(1);
        private AbstractProxyTransaction successor;
        private State prevState;

        // SUCCESSOR + DONE
        private boolean done;

        SuccessorState() {
            super("SUCCESSOR");
        }

        // Synchronize with succession process and return the successor
        AbstractProxyTransaction await() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for latch of {}", successor);
                throw new IllegalStateException(e);
            }
            return successor;
        }

        void finish() {
            latch.countDown();
        }

        State getPrevState() {
            return verifyNotNull(prevState, "Attempted to access previous state, which was not set");
        }

        void setPrevState(final State prevState) {
            verify(this.prevState == null, "Attempted to set previous state to %s when we already have %s", prevState,
                    this.prevState);
            this.prevState = requireNonNull(prevState);
            // We cannot have duplicate successor states, so this check is sufficient
            done = DONE.equals(prevState);
        }

        // To be called from safe contexts, where successor is known to be completed
        AbstractProxyTransaction getSuccessor() {
            return verifyNotNull(successor);
        }

        void setSuccessor(final AbstractProxyTransaction successor) {
            verify(this.successor == null, "Attempted to set successor to %s when we already have %s", successor,
                    this.successor);
            this.successor = requireNonNull(successor);
        }

        boolean isDone() {
            return done;
        }

        void setDone() {
            done = true;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractProxyTransaction.class);
    private static final AtomicIntegerFieldUpdater<AbstractProxyTransaction> SEALED_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractProxyTransaction.class, "sealed");
    private static final AtomicReferenceFieldUpdater<AbstractProxyTransaction, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractProxyTransaction.class, State.class, "state");

    /**
     * Transaction has been open and is being actively worked on.
     */
    private static final State OPEN = new State("OPEN");

    /**
     * Transaction has been sealed by the user, but it has not completed flushing to the backed, yet. This is
     * a transition state, as we are waiting for the user to initiate commit procedures.
     *
     * <p>Since the reconnect mechanics relies on state replay for transactions, this state needs to be flushed into the
     * queue to re-create state in successor transaction (which may be based on different messages as locality may have
     * changed). Hence the transition to {@link #FLUSHED} state needs to be handled in a thread-safe manner.
     */
    private static final State SEALED = new State("SEALED");

    /**
     * Transaction state has been flushed into the queue, i.e. it is visible by the successor and potentially
     * the backend. At this point the transaction does not hold any state besides successful requests, all other state
     * is held either in the connection's queue or the successor object.
     *
     * <p>Transition to this state indicates we have all input from the user we need to initiate the correct commit
     * protocol.
     */
    private static final State FLUSHED = new State("FLUSHED");

    /**
     * Transaction state has been completely resolved, we have received confirmation of the transaction fate from
     * the backend. The only remaining task left to do is finishing up the state cleanup, which is done via purge
     * request. We need to hang on to the transaction until that is done, as we have to make sure backend completes
     * purging its state -- otherwise we could have a leak on the backend.
     */
    private static final State DONE = new State("DONE");

    // Touched from client actor thread only
    private final Deque<Object> successfulRequests = new ArrayDeque<>();
    private final ProxyHistory parent;

    // Accessed from user thread only, which may not access this object concurrently
    private long sequence;

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
     * after a successor was injected, so that it can be properly sealed if we are racing. Further complication comes
     * from lock ordering, where the successor injection works with a locked queue and locks proxy objects -- leading
     * to a potential AB-BA deadlock in case of a naive implementation.
     *
     * For tracking user-visible state we use a single volatile int, which is flipped atomically from 0 to 1 exactly
     * once in {@link AbstractProxyTransaction#seal()}. That keeps common operations fast, as they need to perform
     * only a single volatile read to assert state correctness.
     *
     * For synchronizing client actor (successor-injecting) and user (commit-driving) thread, we keep a separate state
     * variable. It uses pre-allocated objects for fast paths (i.e. no successor present) and a per-transition object
     * for slow paths (when successor is injected/present).
     */
    private volatile int sealed;
    private volatile State state;

    AbstractProxyTransaction(final ProxyHistory parent, final boolean isDone) {
        this.parent = requireNonNull(parent);
        if (isDone) {
            state = DONE;
            // DONE implies previous seal operation completed
            sealed = 1;
        } else {
            state = OPEN;
        }
    }

    final void executeInActor(final Runnable command) {
        parent.context().executeInActor(behavior -> {
            command.run();
            return behavior;
        });
    }

    final ActorRef localActor() {
        return parent.localActor();
    }

    final void incrementSequence(final long delta) {
        sequence += delta;
        LOG.debug("Transaction {} incremented sequence to {}", this, sequence);
    }

    final long nextSequence() {
        final long ret = sequence++;
        LOG.debug("Transaction {} allocated sequence {}", this, ret);
        return ret;
    }

    final void delete(final YangInstanceIdentifier path) {
        checkReadWrite();
        checkNotSealed();
        doDelete(path);
    }

    final void merge(final YangInstanceIdentifier path, final NormalizedNode data) {
        checkReadWrite();
        checkNotSealed();
        doMerge(path, data);
    }

    final void write(final YangInstanceIdentifier path, final NormalizedNode data) {
        checkReadWrite();
        checkNotSealed();
        doWrite(path, data);
    }

    final FluentFuture<Boolean> exists(final YangInstanceIdentifier path) {
        checkNotSealed();
        return doExists(path);
    }

    final FluentFuture<Optional<NormalizedNode>> read(final YangInstanceIdentifier path) {
        checkNotSealed();
        return doRead(path);
    }

    final void enqueueRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        LOG.debug("Transaction proxy {} enqueing request {} callback {}", this, request, callback);
        parent.enqueueRequest(request, callback, enqueuedTicks);
    }

    final void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        LOG.debug("Transaction proxy {} sending request {} callback {}", this, request, callback);
        parent.sendRequest(request, callback);
    }

    /**
     * Seal this transaction before it is either committed or aborted. This method should only be invoked from
     * application thread.
     */
    final void seal() {
        // Transition user-visible state first
        final boolean success = markSealed();
        checkState(success, "Proxy %s was already sealed", getIdentifier());

        if (!sealAndSend(OptionalLong.empty())) {
            sealSuccessor();
        }
    }

    /**
     * Internal seal propagation method, invoked when we have raced with reconnection thread. Note that there may have
     * been multiple reconnects, so we have to make sure the action is propagate through all intermediate instances.
     */
    private void sealSuccessor() {
        // Slow path: wait for the successor to complete
        final AbstractProxyTransaction successor = awaitSuccessor();

        // At this point the successor has completed transition and is possibly visible by the user thread, which is
        // still stuck here. The successor has not seen final part of our state, nor the fact it is sealed.
        // Propagate state and seal the successor.
        final Optional<ModifyTransactionRequest> optState = flushState();
        if (optState.isPresent()) {
            forwardToSuccessor(successor, optState.orElseThrow(), null);
        }
        successor.predecessorSealed();
    }

    private void predecessorSealed() {
        if (markSealed() && !sealAndSend(OptionalLong.empty())) {
            sealSuccessor();
        }
    }

    /**
     * Seal this transaction. If this method reports false, the caller needs to deal with propagating the seal operation
     * towards the successor.
     *
     * @return True if seal operation was successful, false if this proxy has a successor.
     */
    boolean sealOnly() {
        return sealState();
    }

    /**
     * Seal this transaction and potentially send it out towards the backend. If this method reports false, the caller
     * needs to deal with propagating the seal operation towards the successor.
     *
     * @param enqueuedTicks Enqueue ticks when this is invoked from replay path.
     * @return True if seal operation was successful, false if this proxy has a successor.
     */
    boolean sealAndSend(final OptionalLong enqueuedTicks) {
        return sealState();
    }

    private boolean sealState() {
        parent.onTransactionSealed(this);
        // Transition internal state to sealed and detect presence of a successor
        return STATE_UPDATER.compareAndSet(this, OPEN, SEALED);
    }

    /**
     * Mark this proxy as having been sealed.
     *
     * @return True if this call has transitioned to sealed state.
     */
    final boolean markSealed() {
        return SEALED_UPDATER.compareAndSet(this, 0, 1);
    }

    private void checkNotSealed() {
        checkState(sealed == 0, "Transaction %s has already been sealed", getIdentifier());
    }

    private void checkSealed() {
        checkState(sealed != 0, "Transaction %s has not been sealed yet", getIdentifier());
    }

    private SuccessorState getSuccessorState() {
        final var local = state;
        if (local instanceof SuccessorState successor) {
            return successor;
        }
        throw new VerifyException("State " + local + " has unexpected class");
    }

    private void checkReadWrite() {
        if (isSnapshotOnly()) {
            throw new UnsupportedOperationException("Transaction " + getIdentifier() + " is a read-only snapshot");
        }
    }

    final void recordSuccessfulRequest(final @NonNull TransactionRequest<?> req) {
        successfulRequests.add(verifyNotNull(req));
    }

    final void recordFinishedRequest(final Response<?, ?> response) {
        if (successfulRequests.peekLast() instanceof IncrementSequence incr) {
            incr.incrementDelta();
        } else {
            successfulRequests.addLast(new IncrementSequence(response.getSequence()));
        }
    }

    /**
     * Abort this transaction. This is invoked only for read-only transactions and will result in an explicit message
     * being sent to the backend.
     */
    final void abort() {
        checkNotSealed();
        parent.abortTransaction(this);

        sendRequest(abortRequest(), resp -> {
            LOG.debug("Transaction {} abort completed with {}", getIdentifier(), resp);
            enqueuePurge();
        });
    }

    final void abort(final VotingFuture<Empty> ret) {
        checkSealed();

        sendDoAbort(t -> {
            if (t instanceof TransactionAbortSuccess) {
                ret.voteYes();
            } else if (t instanceof RequestFailure) {
                ret.voteNo(((RequestFailure<?, ?>) t).getCause().unwrap());
            } else {
                ret.voteNo(unhandledResponseException(t));
            }

            // This is a terminal request, hence we do not need to record it
            LOG.debug("Transaction {} abort completed", this);
            enqueuePurge();
        });
    }

    final void enqueueAbort(final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        checkNotSealed();
        parent.abortTransaction(this);

        enqueueRequest(abortRequest(), resp -> {
            LOG.debug("Transaction {} abort completed with {}", getIdentifier(), resp);
            // Purge will be sent by the predecessor's callback
            if (callback != null) {
                callback.accept(resp);
            }
        }, enqueuedTicks);
    }

    final void enqueueDoAbort(final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        enqueueRequest(new TransactionAbortRequest(getIdentifier(), nextSequence(), localActor()), callback,
            enqueuedTicks);
    }

    final void sendDoAbort(final Consumer<Response<?, ?>> callback) {
        sendRequest(new TransactionAbortRequest(getIdentifier(), nextSequence(), localActor()), callback);
    }

    /**
     * Commit this transaction, possibly in a coordinated fashion.
     *
     * @param coordinated True if this transaction should be coordinated across multiple participants.
     * @return Future completion
     */
    final ListenableFuture<Boolean> directCommit() {
        checkReadWrite();
        checkSealed();

        // Precludes startReconnect() from interfering with the fast path
        synchronized (this) {
            if (STATE_UPDATER.compareAndSet(this, SEALED, FLUSHED)) {
                final SettableFuture<Boolean> ret = SettableFuture.create();
                sendRequest(verifyNotNull(commitRequest(false)), t -> {
                    if (t instanceof TransactionCommitSuccess) {
                        ret.set(Boolean.TRUE);
                    } else if (t instanceof RequestFailure) {
                        final Throwable cause = ((RequestFailure<?, ?>) t).getCause().unwrap();
                        if (cause instanceof ClosedTransactionException) {
                            // This is okay, as it indicates the transaction has been completed. It can happen
                            // when we lose connectivity with the backend after it has received the request.
                            ret.set(Boolean.TRUE);
                        } else {
                            ret.setException(cause);
                        }
                    } else {
                        ret.setException(unhandledResponseException(t));
                    }

                    // This is a terminal request, hence we do not need to record it
                    LOG.debug("Transaction {} directCommit completed", this);
                    enqueuePurge();
                });

                return ret;
            }
        }

        // We have had some interference with successor injection, wait for it to complete and defer to the successor.
        return awaitSuccessor().directCommit();
    }

    final void canCommit(final VotingFuture<?> ret) {
        checkReadWrite();
        checkSealed();

        // Precludes startReconnect() from interfering with the fast path
        synchronized (this) {
            if (STATE_UPDATER.compareAndSet(this, SEALED, FLUSHED)) {
                final var req = verifyNotNull(commitRequest(true));

                sendRequest(req, t -> {
                    switch (t) {
                        case TransactionCanCommitSuccess success -> ret.voteYes();
                        case RequestFailure<?, ?> failure -> ret.voteNo(failure.getCause().unwrap());
                        default -> ret.voteNo(unhandledResponseException(t));
                    }
                    recordSuccessfulRequest(req);
                    LOG.debug("Transaction {} canCommit completed", this);
                });

                return;
            }
        }

        // We have had some interference with successor injection, wait for it to complete and defer to the successor.
        awaitSuccessor().canCommit(ret);
    }

    private AbstractProxyTransaction awaitSuccessor() {
        return getSuccessorState().await();
    }

    final void preCommit(final VotingFuture<?> ret) {
        checkReadWrite();
        checkSealed();

        final var req = new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor());
        sendRequest(req, t -> {
            switch (t) {
                case TransactionPreCommitSuccess success -> ret.voteYes();
                case RequestFailure<?, ?> failure -> ret.voteNo(failure.getCause().unwrap());
                default -> ret.voteNo(unhandledResponseException(t));
            }
            onPreCommitComplete(req);
        });
    }

    private void onPreCommitComplete(final TransactionRequest<?> req) {
        /*
         * The backend has agreed that the transaction has entered PRE_COMMIT phase, meaning it will be committed
         * to storage after the timeout completes.
         *
         * All state has been replicated to the backend, hence we do not need to keep it around. Retain only
         * the precommit request, so we know which request to use for resync.
         */
        LOG.debug("Transaction {} preCommit completed, clearing successfulRequests", this);
        successfulRequests.clear();

        // TODO: this works, but can contain some useless state (like batched operations). Create an empty
        //       equivalent of this request and store that.
        recordSuccessfulRequest(req);
    }

    final void doCommit(final VotingFuture<?> ret) {
        checkReadWrite();
        checkSealed();

        sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), t -> {
            switch (t) {
                case TransactionCommitSuccess success -> ret.voteYes();
                case RequestFailure<?, ?> failure -> ret.voteNo(failure.getCause().unwrap());
                default -> ret.voteNo(unhandledResponseException(t));
            }
            LOG.debug("Transaction {} doCommit completed", this);

            // Needed for ProxyHistory$Local data tree rebase points.
            parent.completeTransaction(this);

            enqueuePurge();
        });
    }

    private void enqueuePurge() {
        enqueuePurge(null);
    }

    final void enqueuePurge(final Consumer<Response<?, ?>> callback) {
        // Purge request are dispatched internally, hence should not wait
        enqueuePurge(callback, parent.currentTime());
    }

    final void enqueuePurge(final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        LOG.debug("{}: initiating purge", this);

        final var prev = state;
        if (prev instanceof SuccessorState successor) {
            successor.setDone();
        } else if (!STATE_UPDATER.compareAndSet(this, prev, DONE)) {
            LOG.warn("{}: moved from state {} while we were purging it", this, prev);
        }

        successfulRequests.clear();

        enqueueRequest(new TransactionPurgeRequest(getIdentifier(), nextSequence(), localActor()), resp -> {
            LOG.debug("{}: purge completed", this);
            parent.purgeTransaction(this);

            if (callback != null) {
                callback.accept(resp);
            }
        }, enqueuedTicks);
    }

    // Called with the connection unlocked
    final synchronized void startReconnect() {
        // At this point canCommit/directCommit are blocked, we assert a new successor state, retrieving the previous
        // state. This method is called with the queue still unlocked.
        final var nextState = new SuccessorState();
        final var prevState = STATE_UPDATER.getAndSet(this, nextState);

        LOG.debug("Start reconnect of proxy {} previous state {}", this, prevState);
        if (prevState instanceof SuccessorState successor) {
            throw new VerifyException("Proxy " + this + " duplicate reconnect attempt after " + successor);
        }

        // We have asserted a slow-path state, seal(), canCommit(), directCommit() are forced to slow paths, which will
        // wait until we unblock nextState's latch before accessing state. Now we record prevState for later use and we
        // are done.
        nextState.setPrevState(prevState);
    }

    // Called with the connection locked
    final void replayMessages(final ProxyHistory successorHistory, final Iterable<ConnectionEntry> enqueuedEntries) {
        final var local = getSuccessorState();
        final var prevState = local.getPrevState();

        final var successor = successorHistory.createTransactionProxy(getIdentifier(), isSnapshotOnly(),
            local.isDone());
        LOG.debug("{} created successor {}", this, successor);
        local.setSuccessor(successor);

        // Replay successful requests first
        if (!successfulRequests.isEmpty()) {
            // We need to find a good timestamp to use for successful requests, as we do not want to time them out
            // nor create timing inconsistencies in the queue -- requests are expected to be ordered by their enqueue
            // time. We will pick the time of the first entry available. If there is none, we will just use current
            // time, as all other requests will get enqueued afterwards.
            final var firstInQueue = Iterables.getFirst(enqueuedEntries, null);
            final long now = firstInQueue != null ? firstInQueue.getEnqueuedTicks() : parent.currentTime();

            for (var obj : successfulRequests) {
                switch (obj) {
                    case TransactionRequest<?> req -> {
                        LOG.debug("Forwarding successful request {} to successor {}", req, successor);
                        successor.doReplayRequest(req, resp -> { /*NOOP*/ }, now);
                    }
                    case IncrementSequence req -> {
                        successor.doReplayRequest(new IncrementTransactionSequenceRequest(getIdentifier(),
                            req.getSequence(), localActor(), isSnapshotOnly(), req.getDelta()), resp -> { /*NOOP*/ },
                            now);
                        LOG.debug("Incrementing sequence {} to successor {}", obj, successor);
                    }
                    default -> throw new VerifyException("Unexpected request " + obj);
                }
            }
            LOG.debug("{} replayed {} successful requests", getIdentifier(), successfulRequests.size());
            successfulRequests.clear();
        }

        // Now replay whatever is in the connection
        final var it = enqueuedEntries.iterator();
        while (it.hasNext()) {
            final var eentry = it.next();
            final var req = eentry.getRequest();

            if (getIdentifier().equals(req.getTarget())) {
                if (req instanceof TransactionRequest<?> tx) {
                    LOG.debug("Replaying queued request {} to successor {}", req, successor);
                    successor.doReplayRequest(tx, eentry.getCallback(), eentry.getEnqueuedTicks());
                    it.remove();
                } else {
                    throw new VerifyException("Unhandled request " + req);
                }
            }
        }

        /*
         * Check the state at which we have started the reconnect attempt. State transitions triggered while we were
         * reconnecting have been forced to slow paths, which will be unlocked once we unblock the state latch
         * at the end of this method.
         */
        if (SEALED.equals(prevState)) {
            LOG.debug("Proxy {} reconnected while being sealed, propagating state to successor {}", this, successor);
            final long enqueuedTicks = parent.currentTime();
            flushState().ifPresent(toFlush -> successor.handleReplayedRemoteRequest(toFlush, null, enqueuedTicks));
            if (successor.markSealed()) {
                successor.sealAndSend(OptionalLong.of(enqueuedTicks));
            }
        }
    }

    /**
     * Invoked from {@link #replayMessages(AbstractProxyTransaction, Iterable)} to have successor adopt an in-flight
     * request.
     *
     * <p>Note: this method is invoked by the predecessor on the successor.
     *
     * @param request Request which needs to be forwarded
     * @param callback Callback to be invoked once the request completes
     * @param enqueuedTicks ticker-based time stamp when the request was enqueued
     */
    private void doReplayRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        if (request instanceof AbstractLocalTransactionRequest<?> req) {
            handleReplayedLocalRequest(req, callback, enqueuedTicks);
        } else {
            handleReplayedRemoteRequest(request, callback, enqueuedTicks);
        }
    }

    // Called with the connection locked
    final void finishReconnect() {
        final SuccessorState local = getSuccessorState();
        LOG.debug("Finishing reconnect of proxy {}", this);

        // All done, release the latch, unblocking seal() and canCommit() slow paths
        local.finish();
    }

    /**
     * Invoked from a retired connection for requests which have been in-flight and need to be re-adjusted
     * and forwarded to the successor connection.
     *
     * @param request Request to be forwarded
     * @param callback Original callback
     */
    final void forwardRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        forwardToSuccessor(getSuccessorState().getSuccessor(), request, callback);
    }

    final void forwardToSuccessor(final AbstractProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        switch (successor) {
            case LocalProxyTransaction local -> forwardToLocal(local, request, callback);
            case RemoteProxyTransaction remote -> forwardToRemote(remote, request, callback);
        }
    }

    final void replayRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        getSuccessorState().getSuccessor().doReplayRequest(request, callback, enqueuedTicks);
    }

    abstract boolean isSnapshotOnly();

    abstract void doDelete(YangInstanceIdentifier path);

    abstract void doMerge(YangInstanceIdentifier path, NormalizedNode data);

    abstract void doWrite(YangInstanceIdentifier path, NormalizedNode data);

    abstract FluentFuture<Boolean> doExists(YangInstanceIdentifier path);

    abstract FluentFuture<Optional<NormalizedNode>> doRead(YangInstanceIdentifier path);

    @Holding("this")
    abstract Optional<ModifyTransactionRequest> flushState();

    abstract TransactionRequest<?> abortRequest();

    abstract TransactionRequest<?> commitRequest(boolean coordinated);

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

    /**
     * Invoked from {@link LocalProxyTransaction} when it replays its successful requests to its successor.
     *
     * <p>Note: this method is invoked by the predecessor on the successor.
     *
     * @param request Request which needs to be forwarded
     * @param callback Callback to be invoked once the request completes
     * @param enqueuedTicks Time stamp to use for enqueue time
     */
    abstract void handleReplayedLocalRequest(AbstractLocalTransactionRequest<?> request,
            @Nullable Consumer<Response<?, ?>> callback, long enqueuedTicks);

    /**
     * Invoked from {@link RemoteProxyTransaction} when it replays its successful requests to its successor.
     *
     * <p>Note: this method is invoked by the predecessor on the successor.
     *
     * @param request Request which needs to be forwarded
     * @param callback Callback to be invoked once the request completes
     * @param enqueuedTicks Time stamp to use for enqueue time
     */
    abstract void handleReplayedRemoteRequest(TransactionRequest<?> request,
            @Nullable Consumer<Response<?, ?>> callback, long enqueuedTicks);

    static final @NonNull IllegalArgumentException unhandledRequest(final TransactionRequest<?> request) {
        return new IllegalArgumentException("Unhandled request " + request);
    }

    private static @NonNull IllegalStateException unhandledResponseException(final Response<?, ?> resp) {
        return new IllegalStateException("Unhandled response " + resp.getClass());
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("identifier", getIdentifier()).add("state", state).toString();
    }
}
