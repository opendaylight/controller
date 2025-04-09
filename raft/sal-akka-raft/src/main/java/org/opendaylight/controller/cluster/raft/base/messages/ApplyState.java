/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Local message sent by a RaftActor to itself to signal state has been applied to the state machine.
 */
@NonNullByDefault
public record ApplyState(@Nullable Identifier identifier, LogEntry entry) implements ControlMessage {
    public ApplyState {
        requireNonNull(entry);
    }
}
