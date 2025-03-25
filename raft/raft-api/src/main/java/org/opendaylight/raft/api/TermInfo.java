/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.management.ConstructorParameters;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Information about a RAFT term. Includes the {@code term} itself and information whether we voted for a member or not.
 *
 * @param term election term
 * @param votedFor the member we have voted for, {@code null} if we have not voted for anyone
 */
// FIXME: term is currently defined as signed, not unsigned as it could be
@NonNullByDefault
public record TermInfo(long term, @Nullable String votedFor) implements Serializable {
    /**
     * Initial {#link TermInfo}. Equivalent to {@code new TermInfo(0)}, e.g. we know there is a first term and we have
     * not voted in it, as far as we can remember.
     */
    public static final TermInfo INITIAL = new TermInfo(0);

    /**
     * Default constructor.
     *
     * @param term election term
     * @param votedFor the member we have voted for, {@code null} if we have not voted for anyone
     */
    @ConstructorParameters({"term", "votedFor"})
    public TermInfo {
        // Nothing else
    }

    /**
     * Convenience shorthand for {@code new TermInfo(term, null)}.
     *
     * @param term tbe term
     */
    public TermInfo(final long term) {
        this(term, null);
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder().append("TermInfo{term=").append(term);
        final var local = votedFor;
        if (local != null) {
            sb.append(", votedFor=").append(local);
        }
        return sb.append('}').toString();
    }

    /**
     * Return the serialization proxy.
     *
     * @return the serialization proxy
     * @throws ObjectStreamException never
     */
    @java.io.Serial
    private Object writeReplace() throws ObjectStreamException {
        return new TIv1(term, votedFor);
    }
}
