/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.JournaledLogEntry;

/**
 * An iterator-like access to traverse entries stored in an {@link EntryStore}.
 */
@NonNullByDefault
public interface EntryLoader extends AutoCloseable {

    @Nullable JournaledLogEntry loadNext();

    @Override
    void close();
}