/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.AbstractBaseMBean;

import java.util.List;

public class ShardManagerInfo extends AbstractBaseMBean implements
    ShardManagerInfoMBean {

    private final String name;
    private final List<String> localShards;

    public ShardManagerInfo(String name, List<String> localShards) {
        this.name = name;
        this.localShards = localShards;
    }


    @Override protected String getMBeanName() {
        return name;
    }

    @Override protected String getMBeanType() {
        return JMX_TYPE_DISTRIBUTED_DATASTORE;
    }

    @Override protected String getMBeanCategory() {
        return JMX_CATEGORY_SHARD_MANAGER;
    }

    public static ShardManagerInfo createShardManagerMBean(String name, List<String> localShards){
        ShardManagerInfo shardManagerInfo = new ShardManagerInfo(name,
            localShards);

        shardManagerInfo.registerMBean();

        return shardManagerInfo;
    }

    @Override public List<String> getLocalShards() {
        return localShards;
    }
}
