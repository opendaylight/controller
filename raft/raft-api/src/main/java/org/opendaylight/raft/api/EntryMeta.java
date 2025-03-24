/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.api;

/**
 * Information about a RAFT log entry. EntryInfo
 */
public interface EntryMeta {
    /**
     * Returns the index of the entry.
     *
     * @return the index
     */
    long index();

    /**
     * Returns the term of the entry.
     *
     * @return the term
     */
    long term();
}
