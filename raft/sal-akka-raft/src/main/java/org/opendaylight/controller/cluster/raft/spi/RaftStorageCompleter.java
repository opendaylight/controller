/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meeting point for dispatching completions from {@link RaftStorage} callbacks in the context of an actor. While
 * {@link ExecuteInSelfActor} is sufficient for purely-asynchronous tasks, {@link EntryStore} has at least two method
 * which require the callback to resolve before we process any other messages.
 *
 * <p>This acts as a replacement for Pekko Persistence's synchronous operations, supported by {@code stash()} mechanics.
 * Rather than relying on Pekko semantics, we have this completer and its queue and trackers. {@link RaftActor} calls
 * {@link #completeUntilSynchronized()} before it starts processing the next message, ensuring any completions are
 * observed before we process any message. {@link RaftActor} also calls {@link #completeUntilSynchronized()} after it
 * has handled a message and before in returns control back to Pekko.
 *
 * <p>At the end of the day, this acknowledges the special relationship {@link RaftActor} has with {@link RaftStorage}:
 * storage operations' completions take precedence over whatever is delivered to the actor via its mailbox.
 */
@NonNullByDefault
public final class RaftStorageCompleter {
    private final class SyncRaftCallback<T> implements RaftCallback<T> {
        private final RaftCallback<T> delegate;

        SyncRaftCallback(final RaftCallback<T> delegate) {
            this.delegate = requireNonNull(delegate);
        }

        @Override
        public void invoke(@Nullable Exception failure, T success) {
            try {
                delegate.invoke(failure, success);
            } finally {
                if (syncCallbacks.remove(this)) {
                    LOG.debug("{}: completed synchronized callback {}", memberId, delegate);
                } else {
                    LOG.warn("{}: remove failed to find synchronized callback for {}", memberId, delegate);
                }
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("delegate", delegate).toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RaftStorageCompleter.class);

    private final Set<SyncRaftCallback<?>> syncCallbacks = ConcurrentHashMap.newKeySet();
    private final ArrayList<Runnable> pending = new ArrayList<>();
    // We expect there to be one producer (via enqueueCompletions) and one logical consumer thread and they are
    // cooperating. Let's be unfair for now and reap the throughput between the two.
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final ExecuteInSelfActor actor;
    private final String memberId;

    /**
     * Default constructor.
     *
     * @param memberId the memberId
     * @param actor the actor servicing this completer
     */
    public RaftStorageCompleter(final String memberId, final ExecuteInSelfActor actor) {
        this.memberId = requireNonNull(memberId);
        this.actor = requireNonNull(actor);
    }

    /**
     * {@return the {@code memberId}}
     */
    public String memberId() {
        return memberId;
    }

    /**
     * Run all enqueued completions until the queue is observed to be empty. Normally this method is called
     * automatically when the queue becomes non-empty via {@link ExecuteInSelfActor#executeInSelf(Runnable)}, but this
     * method is exposed to allow eager calls from the actor itself.
     *
     * <p>Normally we would not expose this method, but in current actor implementation normal delivery of those calls
     * compete with {@link ControlMessage}s and we effectively want completions to preempt any other messages, so as to
     * provide most up-to-date storage state. This improves latency of both forward progress reporting and error
     * propagation.
     *
     * <p><b>WARNING:</b> This method must be invoked from actor containment matching {@link ExecuteInSelfActor}
     * provided to constructor. Bad things will happen otherwise.
     */
    // FIXME: symmetric to above, but capture current thread and then we have something to compare to
    // FIXME: we should also in
    public void completeUntilEmpty() {
        while (true) {
            final List<Runnable> completions;
            lock.lock();
            try {
                if (pending.isEmpty()) {
                    LOG.trace("{}: no completions pending", memberId);
                    return;
                }

                completions = List.copyOf(pending);
                pending.clear();
            } finally {
                lock.unlock();
            }

            runCompletions(completions);
        }
    }

    /**
     * Run all enqueued completions until all callbacks registered with {@link #syncWithCurrentMessage(RaftCallback)}
     * have completed. If there are no such callbacks, this method does nothing.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public void completeUntilSynchronized() throws InterruptedException {
        while (true) {
            final var size = syncCallbacks.size();
            if (size == 0) {
                LOG.trace("{}: no synchronized callbacks", memberId);
                return;
            }

            final List<Runnable> completions;
            lock.lock();
            try {
                while (pending.isEmpty()) {
                    LOG.debug("{}: awaiting more completions to resolve {} synchronized callback(s)", memberId, size);
                    notEmpty.await();
                }

                completions = List.copyOf(pending);
                pending.clear();
            } finally {
                lock.unlock();
            }

            runCompletions(completions);
        }
    }

    private void runCompletions(final List<Runnable> completions) {
        LOG.debug("{}: running {} completion(s)", memberId, completions.size());
        completions.forEach(Runnable::run);
    }

    /**
     * Enqueue a single {@link Runnable} completion.
     *
     * @param completion the completion
     */
    public void enqueueCompletion(final Runnable completion) {
        enqueueCompletionsImpl(List.of(completion));
    }

    /**
     * Enqueue a multiple {@link Runnable} completions.
     *
     * @param completions the completion
     */
    public void enqueueCompletions(final List<Runnable> completions) {
        completions.forEach(Objects::requireNonNull);
        enqueueCompletionsImpl(completions);
    }

    private void enqueueCompletionsImpl(final List<Runnable> completions) {
        final var size = completions.size();
        if (size == 0) {
            return;
        }

        final boolean becameNonEmpty;
        lock.lock();
        try {
            becameNonEmpty = pending.isEmpty();
            verify(pending.addAll(completions));
            if (becameNonEmpty) {
                LOG.debug("{}: {} completion(s) pending", memberId, size);
                // unblock any waiters first
                notEmpty.signal();
            } else {
                LOG.debug("{}: {} more completion(s) pending", memberId, size);
            }
        } finally {
            lock.unlock();
        }

        // schedule a flush without holding the lock
        if (becameNonEmpty) {
            actor.executeInSelf(this::completeUntilEmpty);
        }
    }

    /**
     * Register a {@link RaftCallback} as synchronous with current message processing. {@link RaftActor} will not return
     * back from message handler back to Pekko before this callback is invoked.
     *
     * @param callback the completion
     * @return a wrapping {@link Runnable} to be used in its stead
     */
    // FIXME: only allow this to happen when we have indicated willingness to wait for these, enforcing actor
    //        containment
    <T> RaftCallback<T> syncWithCurrentMessage(final RaftCallback<T> callback) {
        final var bound = new SyncRaftCallback<>(callback);
        verify(syncCallbacks.add(bound));
        return bound;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("memberId", memberId)
            .add("syncCallbacks", syncCallbacks.size())
            .add("actor", actor)
            .toString();
    }
}
