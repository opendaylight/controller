/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding.messages;

import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Sent to the local {@link ShardedDataTreeActor} to notify of a shard
 * removal on the local node. The local actor should then notify the remote nodes of the Removal with {@link PrefixShardRemoved}
 * message.
 */
public class RemovePrefixShard {

    private final DOMDataTreeIdentifier prefix;

    public RemovePrefixShard(final DOMDataTreeIdentifier prefix) {

        this.prefix = prefix;
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }
}
