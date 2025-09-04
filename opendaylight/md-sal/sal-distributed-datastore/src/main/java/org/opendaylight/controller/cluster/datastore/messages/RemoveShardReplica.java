/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

/**
 * A message sent to the ShardManager to dynamically remove a local shard replica available in this node.
 *
 * @param shardName name of the local shard that is to be dynamically removed.
 */
// FIXME: replyTo
@NonNullByDefault
public record RemoveShardReplica(String shardName, MemberName memberName) {
    public RemoveShardReplica {
        requireNonNull(shardName);
        requireNonNull(memberName);
    }
}
