/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager;

import java.util.List;

import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

public class ShardManagerInfo extends AbstractMXBean implements ShardManagerInfoMBean {

    public static String JMX_CATEGORY_SHARD_MANAGER = "ShardManager";

    private final List<String> localShards;

    private boolean syncStatus = false;

    public ShardManagerInfo(String name, String mxBeanType, List<String> localShards) {
        super(name, mxBeanType, JMX_CATEGORY_SHARD_MANAGER);
        this.localShards = localShards;
    }

    public static ShardManagerInfo createShardManagerMBean(String name, String mxBeanType,
            List<String> localShards){
        ShardManagerInfo shardManagerInfo = new ShardManagerInfo(name, mxBeanType, localShards);

        shardManagerInfo.registerMBean();

        return shardManagerInfo;
    }

    @Override
    public List<String> getLocalShards() {
        return localShards;
    }

    @Override
    public boolean getSyncStatus() {
        return this.syncStatus;
    }

    public void setSyncStatus(boolean syncStatus){
        this.syncStatus = syncStatus;
    }
}
