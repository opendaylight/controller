/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

public interface Configuration {

    /**
     * Returns all the shard names that belong on the member by the given name.
     */
    @Nonnull Collection<String> getMemberShardNames(@Nonnull String memberName);

    /**
     * Returns the namespace for the given module name or null if not found.
     */
    @Nullable String getModuleNameFromNameSpace(@Nonnull String nameSpace);

    /**
     * Returns the first shard name corresponding to the given module name or null if none is configured.
     */
    @Nullable String getShardNameForModule(@Nonnull String moduleName);

    /**
     * Returns the member replicas for the given shard name.
     */
    @Nonnull Collection<String> getMembersFromShardName(@Nonnull String shardName);

    /**
     * Returns the ShardStrategy for the given module name or null if the module is not found.
     */
    @Nullable ShardStrategy getStrategyForModule(@Nonnull String moduleName);

    /**
     * Returns all the configured shard names.
     */
    Set<String> getAllShardNames();

    /**
     * Adds a new configuration for a module ansd shard.
     *
     * @param namespace the name space of the module.
     * @param moduleName the name of the module.
     * @param shardName the name of the shard.
     * @param shardStrategyName the name of the sharding strategy (eg "module"). If null the default strategy
     *                          is used.
     * @param shardMemberNames the names of the shard's member replicas.
     */
    void addModuleShardConfiguration(@Nonnull URI namespace, @Nonnull String moduleName, @Nonnull String shardName,
            @Nullable String shardStrategyName, @Nonnull Collection<String> shardMemberNames);

    /**
     * Returns a unique set of all member names configured for all shards.
     */
    Collection<String> getUniqueMemberNamesForAllShards();
}
