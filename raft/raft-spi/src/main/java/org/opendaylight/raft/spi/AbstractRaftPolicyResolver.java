/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base class for {@link RaftPolicyResolver} implementations.
 */
@NonNullByDefault
public abstract class AbstractRaftPolicyResolver implements RaftPolicyResolver {
    /**
     * Default constructor.
     */
    protected AbstractRaftPolicyResolver() {
        // Nothing here
    }

    @Override
    public final Optional<RaftPolicy> findRaftPolicy(final String symbolicName) {
        final var wellKnown = WellKnownRaftPolicy.forSymbolicName(symbolicName);
        return wellKnown != null ? Optional.of(wellKnown)
            : streamPolicies().filter(policy -> symbolicName.equals(policy.symbolicName())).findFirst();
    }

    /**
     * {@return a Stream of known RaftPolicies}
     */
    protected abstract Stream<RaftPolicy> streamPolicies();
}
