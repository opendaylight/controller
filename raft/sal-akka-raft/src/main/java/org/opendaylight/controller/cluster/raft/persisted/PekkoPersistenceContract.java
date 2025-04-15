/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * Marker interface for contracts related to Pekko persistence. Interfaces extending, and classes implementing, this
 * interface are to be removed when {@link RaftActor} stops extending {@link AbstractUntypedPersistentActor}.
 */
@Deprecated(since = "11.0.0", forRemoval = true)
public interface PekkoPersistenceContract {
    // Nothing else
}
