/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.api.rev150407.ShardRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.api.rev150407.ShardRoleChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.clustering.api.rev150407.ShardRoleChangedBuilder;

public class ClusteringServiceTest {

    @Test
    public void testClusteringService() {
        ShardRoleChangedBuilder roleChangedBuilder = (new ShardRoleChangedBuilder())
                .setMemberId("member-1")
                .setNewRole(ShardRole.FOLLOWER)
                .setOldRole(ShardRole.NONE)
                .setShardId("member-1-inventory-operational-shard");

        NotificationProviderService mockNotificationProviderService = Mockito.mock(NotificationProviderService.class);
        ClusteringService clusteringService = new ClusteringServiceImpl(mockNotificationProviderService);

        assertNull(clusteringService.getLastShardRoleChanged("member-1-inventory-operational-shard"));
        clusteringService.notifyShardRoleChange("member-1", "member-1-inventory-operational-shard", ShardRole.NONE, ShardRole.FOLLOWER);
        Mockito.verify(mockNotificationProviderService, Mockito.atLeastOnce()).publish(
                Mockito.eq(roleChangedBuilder.build()), Mockito.any(ExecutorService.class));

        clusteringService.notifyShardRoleChange("member-1", "member-1-inventory-operational-shard", ShardRole.FOLLOWER, ShardRole.CANDIDATE);
        Mockito.verify(mockNotificationProviderService, Mockito.atLeastOnce()).publish(
                Mockito.eq(roleChangedBuilder.setOldRole(ShardRole.FOLLOWER).setNewRole(ShardRole.CANDIDATE).build()),
                Mockito.any(ExecutorService.class));

        clusteringService.notifyShardRoleChange("member-1", "member-1-inventory-operational-shard", ShardRole.CANDIDATE, ShardRole.LEADER);
        Mockito.verify(mockNotificationProviderService, Mockito.atLeastOnce()).publish(
                Mockito.eq(roleChangedBuilder.setOldRole(ShardRole.CANDIDATE).setNewRole(ShardRole.LEADER).build()),
                Mockito.any(ExecutorService.class));

        clusteringService.notifyShardRoleChange("member-1", "member-1-inventory-config-shard", ShardRole.NONE, ShardRole.FOLLOWER);
        Mockito.verify(mockNotificationProviderService, Mockito.atLeastOnce()).publish(
                Mockito.eq(roleChangedBuilder.setShardId("member-1-inventory-config-shard").setOldRole(ShardRole.NONE).setNewRole(ShardRole.FOLLOWER).build()),
                Mockito.any(ExecutorService.class));

        ShardRoleChange shardRoleChange = clusteringService.getLastShardRoleChanged("member-1-inventory-operational-shard");
        assertNotNull(shardRoleChange);
        assertEquals("member-1-inventory-operational-shard", shardRoleChange.getShardId());
        assertEquals(ShardRole.LEADER, shardRoleChange.getNewRole());

        Map<String, ShardRoleChange> shardRoles = clusteringService.getLastShardRoleChangedForAllShards();
        assertEquals(2, shardRoles.size());
        assertEquals(ShardRole.LEADER, shardRoles.get("member-1-inventory-operational-shard").getNewRole());
        assertEquals(ShardRole.FOLLOWER, shardRoles.get("member-1-inventory-config-shard").getNewRole());

    }
}
