/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

/**
 * Encapsulates configuration for a module.
 *
 * @author Thomas Pantelis
 */
public final class ModuleConfig {
    private final String name;
    private final String namespace;
    private final ShardStrategy shardStrategy;
    private final Map<String, ShardConfig> shardConfigs;

    ModuleConfig(final String name, final String namespace, final ShardStrategy shardStrategy,
            final Map<String, ShardConfig> shardConfigs) {
        this.name = requireNonNull(name);
        this.namespace = namespace;
        this.shardStrategy = shardStrategy;
        this.shardConfigs = shardConfigs;
    }

    public @NonNull String getName() {
        return name;
    }

    public @Nullable String getNamespace() {
        return namespace;
    }

    public @Nullable ShardStrategy getShardStrategy() {
        return shardStrategy;
    }

    public @Nullable ShardConfig getShardConfig(final String forName) {
        return shardConfigs.get(forName);
    }

    public @NonNull Collection<ShardConfig> getShardConfigs() {
        return shardConfigs.values();
    }

    public @NonNull Collection<String> getShardNames() {
        return shardConfigs.keySet();
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static Builder builder(final ModuleConfig moduleConfig) {
        return new Builder(moduleConfig);
    }

    public static final class Builder {
        private String name;
        private String nameSpace;
        private ShardStrategy shardStrategy;
        private final Map<String, ShardConfig> shardConfigs = new HashMap<>();

        Builder(final String name) {
            this.name = name;
        }

        private Builder(final ModuleConfig moduleConfig) {
            this.name = moduleConfig.getName();
            this.nameSpace = moduleConfig.getNamespace();
            this.shardStrategy = moduleConfig.getShardStrategy();
            for (ShardConfig shardConfig : moduleConfig.getShardConfigs()) {
                shardConfigs.put(shardConfig.getName(), shardConfig);
            }
        }

        public Builder name(final String newName) {
            this.name = newName;
            return this;
        }

        public Builder nameSpace(final String newNameSpace) {
            this.nameSpace = newNameSpace;
            return this;
        }

        public Builder shardStrategy(final ShardStrategy newShardStrategy) {
            this.shardStrategy = newShardStrategy;
            return this;
        }

        public Builder shardConfig(final String shardName, final Boolean persistent,
                                   final Collection<MemberName> replicas) {
            shardConfigs.put(shardName, new ShardConfig(shardName, persistent, replicas));
            return this;
        }

        public ModuleConfig build() {
            return new ModuleConfig(name, nameSpace, shardStrategy, ImmutableMap.copyOf(shardConfigs));
        }
    }
}
