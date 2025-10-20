/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.AbstractFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;

/**
 * An {@link AbstractFuture} implementation which requires a certain number of votes before it completes. If all votes
 * are 'yes', then it completes with a pre-determined value. If any of the votes are 'no', the future completes with
 * an exception. This exception corresponds to the cause reported by the first 'no' vote, with all subsequent votes
 * added as suppressed exceptions.
 *
 * <p>Implementation is geared toward positive votes. Negative votes have to synchronize and therefore are more likely
 * to see contention.
 *
 * @param <T> Type of value returned on success
 */
class VotingFuture<T> extends AbstractFuture<T> {
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<VotingFuture> VOTES_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(VotingFuture.class, "neededVotes");

    private final T result;

    private final @GuardedBy("failures") Collection<Throwable> failures = new ArrayList<>(0);
    @SuppressWarnings("unused")
    private volatile int neededVotes;

    VotingFuture(final T result, final int requiredVotes) {
        this.result = requireNonNull(result);
        checkArgument(requiredVotes > 0);
        this.neededVotes = requiredVotes;

    }

    void voteYes() {
        if (castVote()) {
            synchronized (failures) {
                resolveResult();
            }
        }
    }

    void voteNo(final Throwable cause) {
        synchronized (failures) {
            failures.add(cause);
            if (castVote()) {
                resolveResult();
            }
        }
    }

    private boolean castVote() {
        final int votes = VOTES_UPDATER.decrementAndGet(this);
        verify(votes >= 0);
        return votes == 0;
    }

    @Holding("failures")
    private void resolveResult() {
        final Iterator<Throwable> it = failures.iterator();
        if (!it.hasNext()) {
            set(result);
            return;
        }

        final Throwable t = it.next();
        while (it.hasNext()) {
            t.addSuppressed(it.next());
        }

        setException(t);
    }
}
