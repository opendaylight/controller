/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Exception thrown from {@link ElectedStateBehavior}s.
 */
@NonNullByDefault
public final class LeaderStateException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public LeaderStateException(final String message) {
        super(requireNonNull(message));
    }

    public LeaderStateException(final String message, final @Nullable Exception cause) {
        super(requireNonNull(message), cause);
    }
}
