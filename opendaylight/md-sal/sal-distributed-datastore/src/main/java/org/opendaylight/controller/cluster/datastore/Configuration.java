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
    List<String> getMemberShardNames(String memberName);
    Optional<String> getModuleNameFromNameSpace(String nameSpace);
    Map<String, ShardStrategy> getModuleNameToShardStrategyMap();
    List<String> getShardNamesFromModuleName(String moduleName);
    List<String> getMembersFromShardName(String shardName);
}
