/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.apache.pekko.actor.ActorRef;

/**
 * LocalShardFound is a message that is sent by the
 * org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager
 * when it finds a shard with the specified name in it's local shard registry.
 */
// FIXME: nullable?
public final class LocalShardFound {
    private final ActorRef path;

    public LocalShardFound(final ActorRef path) {
        this.path = path;
    }

    public ActorRef getPath() {
        return path;
    }
}
