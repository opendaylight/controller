/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public final class TestTermInfoStore implements TermInfoStore {
    private TermInfo current = new TermInfo(1, "");

    @Override
    public TermInfo currentTerm() {
        return current;
    }

    @Override
    public void update(final TermInfo newElectionInfo) {
        current = requireNonNull(newElectionInfo);
    }

    @Override
    public void updateAndPersist(final TermInfo newElectionInfo) {
        update(newElectionInfo);
        // TODO: write to some persistent state?
    }
}
