/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;

/**
 * Represents one entry in the replicated log.
 */
@NonNullByDefault
public interface ReplicatedLogEntry extends LogEntry {
    /**
     * Returns the payload/data to be replicated.
     *
     * @return the payload/data
     */
    @Deprecated(forRemoval = true)
    default Payload getData() {
        return command();
    }

    @Override
    Payload command();

    /**
     * Returns the size of the entry in bytes. An approximate number may be good enough.
     *
     * @return the size of the entry in bytes.
     */
    int size();

    /**
     * Return the estimate of serialized size of this entry when passed through serialization. The estimate needs to
     * be reasonably accurate and should err on the side of caution and report a slightly-higher size in face of
     * uncertainty.
     *
     * @return An estimate of serialized size.
     */
    int serializedSize();

    /**
     * Checks if persistence is pending for this entry.
     *
     * @return true if persistence is pending, false otherwise.
     */
    boolean isPersistencePending();
}
