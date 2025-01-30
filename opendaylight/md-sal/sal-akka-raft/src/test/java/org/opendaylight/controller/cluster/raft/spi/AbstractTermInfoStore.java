/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
abstract class AbstractTermInfoStore implements TermInfoStore {
    private TermInfo current;

    AbstractTermInfoStore() {
        this(TermInfo.INITIAL);
    }

    AbstractTermInfoStore(final TermInfo current) {
        this.current = requireNonNull(current);
    }

    AbstractTermInfoStore(final long term, final @Nullable String votedFor) {
        this(new TermInfo(term, votedFor));
    }

    @Override
    public final TermInfo currentTerm() {
        return current;
    }

    @Override
    public final void setTerm(final TermInfo newElectionInfo) {
        current = requireNonNull(newElectionInfo);
    }

    @Override
    public final @Nullable TermInfo loadAndSetTerm() {
        return null;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("current", current).toString();
    }
}
