/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * Abstract base class for {@link RaftCommand} implementations.
 */
public abstract non-sealed class AbstractStateCommand extends Payload implements StateCommand {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
}
