/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

import java.util.List;
import java.util.Map;

public interface Configuration {

    /**
     * Given a memberName find all the shards that belong on that member and
     * return the names of those shards
     *
     * @param memberName
     * @return
     */
    List<String> getMemberShardNames(String memberName);

    /**
     * Given a module namespace return the name of a module
     * @param nameSpace
     * @return
     */
    Optional<String> getModuleNameFromNameSpace(String nameSpace);

    /**
     * Get a mapping of the module names to it's corresponding ShardStrategy
     * @return
     */
    Map<String, ShardStrategy> getModuleNameToShardStrategyMap();

    /**
     * Given a module name find all the shardNames corresponding to it
     * @param moduleName
     * @return
     */
    List<String> getShardNamesFromModuleName(String moduleName);

    /**
     * Given a shardName find all the members on which it belongs
     *
     * @param shardName
     * @return
     */
    List<String> getMembersFromShardName(String shardName);
}
