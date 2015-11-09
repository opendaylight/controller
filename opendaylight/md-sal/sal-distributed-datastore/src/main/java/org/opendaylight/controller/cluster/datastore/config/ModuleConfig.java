/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

/**
 * Encapsulates configuration for a module.
 *
 * @author Thomas Pantelis
 */
public class ModuleConfig {
    private final String name;
    private String nameSpace;
    private ShardStrategy shardStrategy;
    private final Map<String, ShardConfig> shardConfigs = new HashMap<>();

    public ModuleConfig(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public ShardStrategy getShardStrategy() {
        return shardStrategy;
    }

    public ShardConfig getShardConfig(String name) {
        return shardConfigs.get(name);
    }

    public Collection<ShardConfig> getShardConfigs() {
        return shardConfigs.values();
    }

    public Collection<String> getShardNames() {
        return shardConfigs.keySet();
    }

    public void addShardConfig(String name, Set<String> replicas) {
        shardConfigs.put(name, new ShardConfig(name, replicas));
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public void setShardStrategy(ShardStrategy shardStrategy) {
        this.shardStrategy = shardStrategy;
    }

    public ShardConfig removeShardConfig(String name) {
        return shardConfigs.remove(name);
    }
}
