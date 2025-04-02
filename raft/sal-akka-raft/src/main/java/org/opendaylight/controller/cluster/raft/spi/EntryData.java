/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * A piece of effectively-data carried in an {@link ReplicatedLogEntry}. Each entry is a delta the RAFT Finite State
 * Machine. It can either be a RaftDelta
 */
public sealed interface EntryData extends Immutable permits RaftDelta, StateDelta, Payload {
    // Nothing else
}
