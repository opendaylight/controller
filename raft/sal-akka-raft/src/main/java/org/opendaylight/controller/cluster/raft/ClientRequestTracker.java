/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Consensus forwarding tracker.
 *
 * @param identifier the identifier of the object that is to be replicated. For example a transaction identifier in the
 *        case of a transaction
 * @param logIndex the index of the log entry that is to be replicated
 */
@NonNullByDefault
public record ClientRequestTracker(long logIndex, Identifier identifier) {
    public ClientRequestTracker {
        requireNonNull(identifier);
    }
}
