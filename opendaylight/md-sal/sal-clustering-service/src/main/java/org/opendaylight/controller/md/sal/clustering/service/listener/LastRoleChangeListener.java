/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.clustering.service.listener;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.api.rev150407.ShardRoleChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.service.provider.rev150407.SalClusteringServiceProviderListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.service.provider.rev150407.ShardRoleChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LastRoleChangeListener implements SalClusteringServiceProviderListener {
    private static final Logger LOG = LoggerFactory.getLogger(LastRoleChangeListener.class);

    private ConcurrentHashMap<String, ShardRoleChange> mapLastRole = new ConcurrentHashMap<>();

    @Override
    public void onShardRoleChanged(ShardRoleChanged notification) {
        LOG.info("RoleChanged Notification received from broker:{}", notification);
        mapLastRole.put(notification.getShardId(), notification);

    }

    public ShardRoleChange getLastRoleChanged(String shardId) {
        return mapLastRole.get(shardId);
    }

    public Map<String, ShardRoleChange> getLastRoleChanges() {
        return ImmutableMap.copyOf(mapLastRole);
    }
}
