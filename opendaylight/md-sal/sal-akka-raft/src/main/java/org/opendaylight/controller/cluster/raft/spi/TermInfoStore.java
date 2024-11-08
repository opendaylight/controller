/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Storage for {@link TermInfo}. Provides access to current term and updates to it both transient, via
 * {@link #update(TermInfo)} and persistent, via {@link #updateAndPersist(TermInfo)}.
 */
@Beta
@NonNullByDefault
public interface TermInfoStore {
    /**
     * Returns {@link TermInfo} for current term.
     *
     * @return {@link TermInfo} for current term
     */
    // FIXME: really non-null? how does this start off?
    TermInfo currentTerm();

    /**
     * This method updates the in-memory election term state. This method should be called when recovering election
     * state from persistent storage.
     *
     * @param newTerm new {@link TermInfo}
     */
    void update(TermInfo newTerm);

    /**
     * This method updates the in-memory election term state and persists it so it can be recovered on next restart.
     * This method should be called when starting a new election or when a Raft RPC message is received  with a higher
     * term.
     *
     * @param newTerm new {@link TermInfo}
     */
    void updateAndPersist(TermInfo newTerm);
}
