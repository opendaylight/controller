/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Storage for {@link TermInfo}. Provides access to current term and updates to it both transient, via
 * {@link #setTerm(TermInfo)} and persistent, via {@link #storeAndSetTerm(TermInfo)}.
 */
@NonNullByDefault
public interface TermInfoStore {
    /**
     * Returns {@link TermInfo} for current term. Freshly-initialized instances return {@link TermInfo#INITIAL}.
     *
     * @return {@link TermInfo} for current term
     */
    TermInfo currentTerm();

    /**
     * This method updates the in-memory election term state. This method should be called when recovering election
     * state from persistent storage.
     *
     * @param newTerm new {@link TermInfo}
     */
    void setTerm(TermInfo newTerm);

    /**
     * This method updates the in-memory election term state and persists it so it can be recovered on next restart.
     * This method should be called when starting a new election or when a Raft RPC message is received  with a higher
     * term.
     *
     * @param newTerm new {@link TermInfo}
     * @throws IOException if an I/O error occurs
     */
    void storeAndSetTerm(TermInfo newTerm) throws IOException;

    /**
     * Attempt to load the last persisted {@link TermInfo}, if available. If successful, {@link #currentTerm()} is
     * updated.
     *
     * @return the persisted {@link TermInfo}, or {@code null} of none is available
     * @throws IOException if an I/O error occurs
     */
    @Nullable TermInfo loadAndSetTerm() throws IOException;
}
