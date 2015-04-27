/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.api;


import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.api.rev150407.ShardRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.api.rev150407.ShardRoleChange;

/**
 * Service which provides api for cluster roles.
 */
public interface ClusteringRoleService {

    /**
     * Notify clustering service of the shard role change.
     * @param memberId
     * @param shardId
     * @param oldRole
     * @param newRole
     */
    void notifyShardRoleChange(String memberId, String shardId, ShardRole oldRole, ShardRole newRole);

    /**
     * Get the last role change for the given shard
     * An app which relies on the roles of a shard, can make use of this api to get the latest roles
     * @param shardId
     * @return
     */
    ShardRoleChange getLastShardRoleChanged(String shardId);

    /**
     * Get the latest of all shard role changes.
     *
     * An app which relies on the roles of a shard, can make use of this api to get the latest roles
     *
     * @return
     */
    Map<String, ShardRoleChange> getLastShardRoleChangedForAllShards();

}
