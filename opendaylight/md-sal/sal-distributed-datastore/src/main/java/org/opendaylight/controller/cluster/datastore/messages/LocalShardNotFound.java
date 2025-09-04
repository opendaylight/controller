/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

/**
 * LocalShardNotFound is a message that is sent by the
 * org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager
 * when it cannot locate a shard in it's local registry with the shardName specified.
 */
// FIXME: nullable?
public final class LocalShardNotFound {
    private final String shardName;

    /**
     * Constructs an instance.
     *
     * @param shardName the name of the shard that could not be found
     */
    public LocalShardNotFound(final String shardName) {
        this.shardName = shardName;
    }

    public String getShardName() {
        return shardName;
    }
}
