/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

/**
 * Encapsulates configuration for a module.
 *
 * @author Thomas Pantelis
 */
public class ModuleConfig {
    private final String name;
    private final String nameSpace;
    private final ShardStrategy shardStrategy;
    private final Map<String, ShardConfig> shardConfigs;

    private ModuleConfig(String name, String nameSpace, ShardStrategy shardStrategy,
            Map<String, ShardConfig> shardConfigs) {
        this.name = name;
        this.nameSpace = nameSpace;
        this.shardStrategy = shardStrategy;
        this.shardConfigs = shardConfigs;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getNameSpace() {
        return nameSpace;
    }

    @Nullable
    public ShardStrategy getShardStrategy() {
        return shardStrategy;
    }

    @Nullable
    public ShardConfig getShardConfig(String name) {
        return shardConfigs.get(name);
    }

    @Nonnull
    public Collection<ShardConfig> getShardConfigs() {
        return shardConfigs.values();
    }

    @Nonnull
    public Collection<String> getShardNames() {
        return shardConfigs.keySet();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(ModuleConfig moduleConfig) {
        return new Builder(moduleConfig);
    }

    public static class Builder {
        private String name;
        private String nameSpace;
        private ShardStrategy shardStrategy;
        private final Map<String, ShardConfig> shardConfigs = new HashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        private Builder(ModuleConfig moduleConfig) {
            this.name = moduleConfig.getName();
            this.nameSpace = moduleConfig.getNameSpace();
            this.shardStrategy = moduleConfig.getShardStrategy();
            for (ShardConfig shardConfig : moduleConfig.getShardConfigs()) {
                shardConfigs.put(shardConfig.getName(), shardConfig);
            }
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder nameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }

        public Builder shardStrategy(ShardStrategy shardStrategy) {
            this.shardStrategy = shardStrategy;
            return this;
        }

        public Builder shardConfig(String name, Collection<String> replicas) {
            shardConfigs.put(name, new ShardConfig(name, replicas));
            return this;
        }

        public ModuleConfig build() {
            return new ModuleConfig(Preconditions.checkNotNull(name), nameSpace, shardStrategy,
                    ImmutableMap.copyOf(shardConfigs));
        }
    }
}
