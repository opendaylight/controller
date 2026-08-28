/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.AbstractFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
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
final class VotingFuture<T> extends AbstractFuture<T> {
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(VotingFuture.class, "neededVotes", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final @GuardedBy("failures") Collection<Throwable> failures = new ArrayList<>(0);
    private final T result;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile int neededVotes;

    VotingFuture(final T result, final int requiredVotes) {
        this.result = requireNonNull(result);
        checkArgument(requiredVotes > 0);
        neededVotes = requiredVotes;

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
        final int prevNeeded = (int) VH.getAndAdd(this, -1);
        if (prevNeeded < 1) {
            throw new VerifyException("neededVotes underflow");
        }
        return prevNeeded == 1;
    }

    @Holding("failures")
    private void resolveResult() {
        final var it = failures.iterator();
        if (!it.hasNext()) {
            set(result);
            return;
        }

        final var t = it.next();
        while (it.hasNext()) {
            t.addSuppressed(it.next());
        }
        setException(t);
    }
}
