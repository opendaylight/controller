/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;

/**
 * The FindPrimary message is used to locate the primary of any given shard
 *
 * TODO : Make this serializable
 */
public class FindPrimary{
    private final String shardName;

    public FindPrimary(String shardName){

        Preconditions.checkNotNull(shardName, "shardName should not be null");

        this.shardName = shardName;
    }

    public String getShardName() {
        return shardName;
    }
}
