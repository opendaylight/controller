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
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

/**
 * Encapsulates configuration for a module.
 *
 * @author Thomas Pantelis
 */
public class ModuleConfig {
    private final String name;
    private final String namespace;
    private final ShardStrategy shardStrategy;
    private final Map<String, ShardConfig> shardConfigs;

    private ModuleConfig(String name, String namespace, ShardStrategy shardStrategy,
            Map<String, ShardConfig> shardConfigs) {
        this.name = name;
        this.namespace = namespace;
        this.shardStrategy = shardStrategy;
        this.shardConfigs = shardConfigs;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @Nullable
    public ShardStrategy getShardStrategy() {
        return shardStrategy;
    }

    @Nullable
    public ShardConfig getShardConfig(String forName) {
        return shardConfigs.get(forName);
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
            this.nameSpace = moduleConfig.getNamespace();
            this.shardStrategy = moduleConfig.getShardStrategy();
            for (ShardConfig shardConfig : moduleConfig.getShardConfigs()) {
                shardConfigs.put(shardConfig.getName(), shardConfig);
            }
        }

        public Builder name(String newName) {
            this.name = newName;
            return this;
        }

        public Builder nameSpace(String newNameSpace) {
            this.nameSpace = newNameSpace;
            return this;
        }

        public Builder shardStrategy(ShardStrategy newShardStrategy) {
            this.shardStrategy = newShardStrategy;
            return this;
        }

        public Builder shardConfig(String shardName, Collection<MemberName> replicas) {
            shardConfigs.put(shardName, new ShardConfig(shardName, replicas));
            return this;
        }

        public ModuleConfig build() {
            return new ModuleConfig(Preconditions.checkNotNull(name), nameSpace, shardStrategy,
                    ImmutableMap.copyOf(shardConfigs));
        }
    }
}
