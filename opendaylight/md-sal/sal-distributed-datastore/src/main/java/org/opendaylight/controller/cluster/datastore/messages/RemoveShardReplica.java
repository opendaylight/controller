/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

/**
 * A message sent to the ShardManager to dynamically remove a local shard
 *  replica available in this node.
 */
public class RemoveShardReplica {

    private final String shardName;
    private final MemberName memberName;

    /**
     * Constructor.
     *
     * @param shardName name of the local shard that is to be dynamically removed.
     */
    public RemoveShardReplica(@Nonnull String shardName, @Nonnull MemberName memberName) {
        this.shardName = Preconditions.checkNotNull(shardName, "shardName should not be null");
        this.memberName = Preconditions.checkNotNull(memberName, "memberName should not be null");
    }

    public String getShardName() {
        return shardName;
    }

    public MemberName getMemberName() {
        return memberName;
    }

    @Override
    public String toString() {
        return "RemoveShardReplica [shardName=" + shardName + ", memberName=" + memberName + "]";
    }
}
