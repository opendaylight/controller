/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

/**
 * The FindPrimary message is used to locate the primary of any given shard.
 */
public final class FindPrimary implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String shardName;
    private final boolean waitUntilReady;

    public FindPrimary(final String shardName, final boolean waitUntilReady) {
        this.shardName = requireNonNull(shardName, "shardName should not be null");
        this.waitUntilReady = waitUntilReady;
    }

    public String getShardName() {
        return shardName;
    }

    public boolean isWaitUntilReady() {
        return waitUntilReady;
    }

    @Override
    public String toString() {
        return getClass().getName() + " [shardName=" + shardName + ", waitUntilReady=" + waitUntilReady + "]";
    }
}
