/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import org.opendaylight.controller.cluster.raft.persisted.MigratedSerializable;

/**
 * Marker interface for migrated payloads.
 *
 * @deprecated This is a migration construct and needs to be removed when we remove support for
 *             {@link PayloadVersion#MAGNESIUM}.
 */
@Deprecated(since = "7.0.0", forRemoval = true)
interface MagnesiumPayload extends MigratedSerializable {
    @Override
    default boolean isMigrated() {
        return true;
    }
}
