/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Well-known {@link RaftPolicy} implementations.
 */
@NonNullByDefault
public enum WellKnownRaftPolicy implements RaftPolicy {
    /**
     * Normal policy: require cluster-wide consensus before applying state and trigger new elections on heartbeat
     * timeout.
     */
    NORMAL("default", true, false),
    /**
     * Manual policy: require cluster-wide consensus before applying state, but do not initiate elections on heartbeat
     * timeout.
     */
    DISABLE_ELECTIONS("disable-elections", false, false),
    /**
     * A policy intended to be used in a two node deployment where when one instance of the controller goes down the
     * other instance is to take over and move the state forward. When this policy is used Raft elections are disabled.
     * This is primarily because we would need to specify the leader externally. Also since we want one node to continue
     * to function while the other node is down we would need to apply a modification to the state before consensus
     * occurs.
     */
    TWO_NODE_CLUSTER("two-node-cluster", false, true);

    private final String symbolicName;
    private final boolean automaticElectionsEnabled;
    private final boolean applyModificationToStateBeforeConsensus;

    WellKnownRaftPolicy(final String symbolicName, final boolean automaticElectionsEnabled,
            final boolean applyModificationToStateBeforeConsensus) {
        this.symbolicName = requireNonNull(symbolicName);
        this.automaticElectionsEnabled = automaticElectionsEnabled;
        this.applyModificationToStateBeforeConsensus = applyModificationToStateBeforeConsensus;
    }

    /**
     * {@return the WellKnownRaftPolicy corresponding to a symbolic name, or {@code null}}
     * @param symbolicName the symbolic name
     */
    public static @Nullable WellKnownRaftPolicy forSymbolicName(final String symbolicName) {
        return switch (symbolicName) {
            case "default" -> NORMAL;
            case "disable-elections" -> DISABLE_ELECTIONS;
            default -> null;
        };
    }

    @Override
    public String symbolicName() {
        return symbolicName;
    }

    @Override
    public boolean automaticElectionsEnabled() {
        return automaticElectionsEnabled;
    }

    @Override
    public boolean applyModificationToStateBeforeConsensus() {
        return applyModificationToStateBeforeConsensus;
    }
}
