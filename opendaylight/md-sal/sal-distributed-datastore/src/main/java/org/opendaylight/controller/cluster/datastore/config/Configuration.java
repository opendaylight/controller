/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.config;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

public interface Configuration {

    /**
     * Returns all the shard names that belong on the member by the given name.
     */
    @Nonnull Collection<String> getMemberShardNames(@Nonnull MemberName memberName);

    /**
     * Returns the module name for the given namespace name or null if not found.
     */
    @Nullable String getModuleNameFromNameSpace(@Nonnull String nameSpace);

    /**
     * Returns the first shard name corresponding to the given module name or null if none is configured.
     */
    @Nullable String getShardNameForModule(@Nonnull String moduleName);

    /**
     * Return the shard name corresponding to the prefix, or null if none is configured.
     */
    @Nullable String getShardNameForPrefix(@Nonnull DOMDataTreeIdentifier prefix);

    /**
     * Returns the member replicas for the given shard name.
     */
    @Nonnull Collection<MemberName> getMembersFromShardName(@Nonnull String shardName);

    /**
     * Returns the ShardStrategy for the given module name or null if the module is not found.
     */
    @Nullable ShardStrategy getStrategyForModule(@Nonnull String moduleName);

    /**
     * Returns all the configured shard names.
     */
    Set<String> getAllShardNames();

    /**
     * Adds a new configuration for a module and shard.
     */
    void addModuleShardConfiguration(@Nonnull ModuleShardConfiguration config);

    /**
     * Adds a new configuration for a shard based on prefix.
     */
    void addPrefixShardConfiguration(@Nonnull PrefixShardConfiguration config);

    /**
     * Removes a shard configuration for the specified prefix.
     */
    void removePrefixShardConfiguration(@Nonnull DOMDataTreeIdentifier prefix);

    /**
     * Returns the configuration for all configured prefix shards.
     *
     * @return An immutable copy of the currently configured prefix shards.
     */
    Map<DOMDataTreeIdentifier, PrefixShardConfiguration> getAllPrefixShardConfigurations();

    /**
     * Returns a unique set of all member names configured for all shards.
     */
    Collection<MemberName> getUniqueMemberNamesForAllShards();

    /*
     * Verifies if the given module shard in available in the cluster
     */
    boolean isShardConfigured(String shardName);

    /**
     * Adds the given member as the new replica for the given shardName.
     */
    void addMemberReplicaForShard(String shardName, MemberName memberName);

    /**
     * Removes the given member as a replica for the given shardName.
     */
    void removeMemberReplicaForShard(String shardName, MemberName memberName);

    /**
     * Returns the ShardStrategy for the given prefix or null if the prefix is not found.
     */
    @Nullable ShardStrategy getStrategyForPrefix(@Nonnull DOMDataTreeIdentifier prefix);
}
