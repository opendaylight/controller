/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.clustering.service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.clustering.service.listener.LastRoleChangeListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRoleChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.service.provider.rev150407.ShardRoleChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.service.provider.rev150407.ShardRoleChangedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteringServiceImpl implements ClusteringService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteringServiceImpl.class);

    private NotificationProviderService notificationProviderService;
    private LastRoleChangeListener lastRoleChangeListener;
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    public ClusteringServiceImpl(NotificationProviderService notificationProviderService,
                                 LastRoleChangeListener lastRoleChangeListener) {
        this.notificationProviderService = notificationProviderService;
        this.lastRoleChangeListener = lastRoleChangeListener;
    }

    @Override
    public void close() throws Exception {
        if(!singleThreadExecutor.isShutdown()) {
            singleThreadExecutor.shutdownNow();
        }
    }

    @Override
    public void changeShardRole(String memberId, String shardId, ShardRole oldRole, ShardRole newRole) {
        LOG.info("changed shard role for {} from {} to {}", shardId, oldRole, newRole);
        ShardRoleChanged roleChangedNotification = new ShardRoleChangedBuilder()
                .setMemberId(memberId)
                .setShardId(shardId)
                .setOldRole(oldRole)
                .setNewRole(newRole)
                .build();
        // we do want the role changes  to get notified sequentially.
        // If the changes are notified unordered, then it can have disastrous effects!!
        notificationProviderService.publish(roleChangedNotification, singleThreadExecutor);
    }

    @Override
    public ShardRoleChange getLastShardRoleChanged(String shardIdSuffix) {
        LOG.debug("Get Last role called for suffix {}", shardIdSuffix);
        for (Map.Entry<String, ShardRoleChange> entry : getLastShardRoleChangedForAllShards().entrySet()) {
            if (entry.getValue().getShardId().endsWith(shardIdSuffix)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Map<String, ShardRoleChange> getLastShardRoleChangedForAllShards() {
        return lastRoleChangeListener.getLastRoleChanges();
    }


}
