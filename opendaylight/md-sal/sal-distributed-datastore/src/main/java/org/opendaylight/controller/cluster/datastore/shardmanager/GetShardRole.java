/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Message sent to the local ShardManager to request the current role for the given shard.
 */
@NonNullByDefault
public record GetShardRole(String shardName, ActorRef replyTo) {
    public GetShardRole{
        requireNonNull(shardName);
        requireNonNull(replyTo);
    }
}
