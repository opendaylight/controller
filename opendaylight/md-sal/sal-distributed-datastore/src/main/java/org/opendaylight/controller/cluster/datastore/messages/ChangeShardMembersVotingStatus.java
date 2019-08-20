/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * A local message sent to the ShardManager to change the raft voting status for members of a shard.
 *
 * @author Thomas Pantelis
 */
public class ChangeShardMembersVotingStatus {
    private final String shardName;
    private final Map<String, Boolean> meberVotingStatusMap;

    public ChangeShardMembersVotingStatus(final String shardName, final Map<String, Boolean> meberVotingStatusMap) {
        this.shardName = requireNonNull(shardName);
        this.meberVotingStatusMap = ImmutableMap.copyOf(meberVotingStatusMap);
    }

    public String getShardName() {
        return shardName;
    }

    public Map<String, Boolean> getMeberVotingStatusMap() {
        return meberVotingStatusMap;
    }

    @Override
    public String toString() {
        return "ChangeShardMembersVotingStatus [shardName=" + shardName + ", meberVotingStatusMap="
                + meberVotingStatusMap + "]";
    }


}
