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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.spi.EntryStore.PersistCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meeting point for dispatching completions of {@link JournalWriteTask} callbacks in the context of an actor. While
 * {@link ExecuteInSelfActor} is sufficient for purely-asynchronous tasks, {@link EntryStore} has a method which has
 * a deferred-but-bounded callback contract.
 */
@NonNullByDefault
public final class EntryStoreCompleter {
    private final class DeferredPersistCallback implements PersistCallback {
        private final PersistCallback delegate;

        DeferredPersistCallback(final PersistCallback delegate) {
            this.delegate = requireNonNull(delegate);
        }

        @Override
        public void invoke(@Nullable Exception failure, Long success) {
            try {
                delegate.invoke(failure, success);
            } finally {
                if (deferred.remove(this)) {
                    LOG.debug("{}: completed deferred callback {}", memberId, this);
                } else {
                    LOG.warn("{}: remove failed to find deferred callback, perhaps we already ran {}", memberId, this);
                }
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("delegate", delegate).toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(EntryStoreCompleter.class);

    private final Set<DeferredPersistCallback> deferred = ConcurrentHashMap.newKeySet();
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
    public EntryStoreCompleter(final String memberId, final ExecuteInSelfActor actor) {
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
     * Register a {@link RaftCallback} as deferred.
     *
     * @param callback the completion
     * @return a wrapping {@link Runnable} to be used in its stead
     */
    // FIXME: only allow this to happen when we have indicated willingness to wait for these, enforcing actor
    //        containment
    public PersistCallback deferCallback(final PersistCallback callback) {
        final var bound = new DeferredPersistCallback(callback);
        verify(deferred.add(bound));
        return bound;
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
    public void completeWhilePending() {
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

    public void completeWhileDeferred() throws InterruptedException {
        while (true) {
            final var size = deferred.size();
            if (size == 0) {
                LOG.trace("{}: no deferred callbacks", memberId);
                return;
            }

            final List<Runnable> completions;
            lock.lock();
            try {
                while (pending.isEmpty()) {
                    LOG.debug("{}: awaiting more completions to resolve {} deferred callback(s)", memberId, size);
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

    public void enqueueCompletion(final Runnable completion) {
        enqueueCompletions(List.of(completion));
    }

    // package-protected on purpose, visible for JournalWriteTask, whom we trust to not give us nulls
    void enqueueCompletions(final List<Runnable> completions) {
        final var size = completions.size();
        if (size == 0) {
            return;
        }

        lock.lock();
        try {
            final var signal = pending.isEmpty();
            verify(pending.addAll(completions));
            if (signal) {
                LOG.debug("{}: {} completion(s) pending", memberId, size);
                // unblock any waiters first
                notEmpty.signal();
                // schedule a flush
                actor.executeInSelf(this::completeWhilePending);
            } else {
                LOG.debug("{}: {} more completion(s) pending", memberId, size);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("memberId", memberId)
            .add("deferred", deferred.size())
            .add("actor", actor)
            .toString();
    }
}
