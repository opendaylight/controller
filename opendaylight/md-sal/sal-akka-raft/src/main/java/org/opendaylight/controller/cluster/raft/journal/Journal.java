/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.journal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.state.ElectedStateBehavior;

/**
 * Access to RAFT journal {@link ElectedStateBehavior}s afforded to when {@link RaftActor} is a RAFT leader.
 */
@NonNullByDefault
public interface Journal {

    void appendEntry(AppendJournal request);
}
