/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

/**
 * FindLocalShard is a message that should be sent to the {@link org.opendaylight.controller.cluster.datastore.ShardManager}
 * when we need to find a reference to a LocalShard
 */
public class FindLocalShard {
    private final String shardName;

    public FindLocalShard(String shardName) {
        this.shardName = shardName;
    }

    public String getShardName() {
        return shardName;
    }
}
