/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

import java.io.Serializable;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Serialization proxy for {@link TermInfo}.
 *
 * @param term log entry term
 * @param votedFor the member we have voted for, {@code null} if we have not voted for anyone
 */
record TIv1(long term, @Nullable String votedFor) implements Serializable {
    @java.io.Serial
    private Object readResolve() {
        return new TermInfo(term, votedFor);
    }
}
