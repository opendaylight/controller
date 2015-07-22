/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * Represents one entry in the replicated log
 */
public interface ReplicatedLogEntry {
    /**
     *
     * @return The payload/data to be replicated
     */
    Payload getData();

    /**
     *
     * @return The term of the entry
     */
    long getTerm();

    /**
     *
     * @return The index of the entry
     */
    long getIndex();

    /**
     *
     * @return The size of the entry in bytes. An approximate number may be good enough.
     */
    int size();
}
