/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Encapsulates information for adding a new module shard configuration.
 *
 * @author Thomas Pantelis
 */
public class ModuleShardConfiguration {
    private final URI namespace;
    private final String moduleName;
    private final String shardName;
    private final String shardStrategyName;
    private final Collection<String> shardMemberNames;

    /**
     * Constructs a new instance.
     *
     * @param namespace the name space of the module.
     * @param moduleName the name of the module.
     * @param shardName the name of the shard.
     * @param shardStrategyName the name of the sharding strategy (eg "module"). If null the default strategy
     *                          is used.
     * @param shardMemberNames the names of the shard's member replicas.
     */
    public ModuleShardConfiguration(@Nonnull URI namespace, @Nonnull String moduleName, @Nonnull String shardName,
            @Nullable String shardStrategyName, @Nonnull Collection<String> shardMemberNames) {
        this.namespace = Preconditions.checkNotNull(namespace, "nameSpace should not be null");
        this.moduleName = Preconditions.checkNotNull(moduleName, "moduleName should not be null");
        this.shardName = Preconditions.checkNotNull(shardName, "shardName should not be null");
        this.shardStrategyName = shardStrategyName;
        this.shardMemberNames = Preconditions.checkNotNull(shardMemberNames, "shardMemberNames");
    }

    public URI getNamespace() {
        return namespace;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getShardName() {
        return shardName;
    }

    public String getShardStrategyName() {
        return shardStrategyName;
    }

    public Collection<String> getShardMemberNames() {
        return shardMemberNames;
    }

    @Override
    public String toString() {
        return "ModuleShardConfiguration [namespace=" + namespace + ", moduleName=" + moduleName + ", shardName="
                + shardName + ", shardMemberNames=" + shardMemberNames + ", shardStrategyName=" + shardStrategyName
                + "]";
    }
}
