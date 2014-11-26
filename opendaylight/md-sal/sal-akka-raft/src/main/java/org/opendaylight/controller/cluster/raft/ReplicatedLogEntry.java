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
     * The data stored in that entry
     *
     * @return
     */
    Payload getData();

    /**
     * The term stored in that entry
     *
     * @return
     */
    long getTerm();

    /**
     * The index of the entry
     *
     * @return
     */
    long getIndex();

    /**
     * The size of the entry in bytes.
     *
     * An approximate number may be good enough.
     *
     * @return
     */
    int size();
}
