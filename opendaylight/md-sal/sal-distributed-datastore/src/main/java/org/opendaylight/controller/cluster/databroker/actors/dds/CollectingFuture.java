/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Verify;
import com.google.common.util.concurrent.AbstractFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import javax.annotation.concurrent.GuardedBy;

class CollectingFuture<T> extends AbstractFuture<T> {
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<CollectingFuture> VOTES_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(CollectingFuture.class, "neededVotes");

    private final T result;

    @GuardedBy("failures")
    private final Collection<Throwable> failures = new ArrayList<>(0);
    @SuppressWarnings("unused")
    private volatile int neededVotes;

    CollectingFuture(final T result, final int requiredVotes) {
        this.neededVotes = requiredVotes;

        // null is okay to allow Void type
        this.result = result;
    }

    private boolean castVote() {
        final int votes = VOTES_UPDATER.decrementAndGet(this);
        Verify.verify(votes >= 0);
        return votes == 0;

    }

    @GuardedBy("failures")
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
}
