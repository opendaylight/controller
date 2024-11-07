/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Information about election of a term.
 *
 * @param term election term
 * @param votedFor the member we have voted for
 */
@NonNullByDefault
public record TermInfo(long term, @Nullable String votedFor) implements Serializable {
    public TermInfo(final long term) {
        this(term, null);
    }

    @Deprecated
    public long getCurrentTerm() {
        return term();
    }

    @Deprecated
    public @Nullable String getVotedFor() {
        return votedFor();
    }
}
