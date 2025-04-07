/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.api.EntryMeta;

/**
 * A single entry in the RAFT log. It is composed of {@link EntryMeta} fields and a {@link #command()}, mandated by
 * the RAFT paper: {@code each entry contains command for state machine, and term when entry was received by leader
 * (first index is 1)}.
 */
@NonNullByDefault
public interface LogEntry extends EntryMeta {
    /**
     * Returns this entry's {@link StateMachineCommand}.
     *
     * @return this entry's {@link StateMachineCommand}
     */
    StateMachineCommand command();
}
