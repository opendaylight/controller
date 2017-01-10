/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

/**
 * Message sent to ShardManager to trigger the creation of shard used to store prefix based configuration for the
 * specified LogicalDatastoreType.
 */
public class CreatePrefixConfigShard {

    private final LogicalDatastoreType type;

    public CreatePrefixConfigShard(final LogicalDatastoreType type) {
        this.type = type;
    }

    public LogicalDatastoreType getType() {
        return type;
    }
}
