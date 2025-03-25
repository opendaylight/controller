/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

/**
 * Marker interface for serializable objects which have been migrated. It implements {@link MigratedSerializable} and
 * always returns {@code true} from {@link #isMigrated()}. This interface is marked as deprecated , as any of its users
 * should also be marked as deprecated.
 */
@Deprecated
public interface LegacySerializable extends MigratedSerializable {
    @Override
    @Deprecated(forRemoval = true)
    default boolean isMigrated() {
        return true;
    }
}
