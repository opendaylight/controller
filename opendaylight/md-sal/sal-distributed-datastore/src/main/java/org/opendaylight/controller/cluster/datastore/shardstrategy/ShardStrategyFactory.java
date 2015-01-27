/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardstrategy;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ShardStrategyFactory {
    private static Map<String, ShardStrategy> moduleNameToStrategyMap =
        new ConcurrentHashMap<>();

    private static final String UNKNOWN_MODULE_NAME = "unknown";
    private static Configuration configuration;


    public static void setConfiguration(final Configuration configuration){
        ShardStrategyFactory.configuration = configuration;
        moduleNameToStrategyMap = configuration.getModuleNameToShardStrategyMap();
    }

    public static ShardStrategy getStrategy(final YangInstanceIdentifier path) {
        Preconditions.checkState(configuration != null, "configuration should not be missing");
        Preconditions.checkNotNull(path, "path should not be null");


        String moduleName = getModuleName(path);
        ShardStrategy shardStrategy = moduleNameToStrategyMap.get(moduleName);
        if (shardStrategy == null) {
            return DefaultShardStrategy.getInstance();
        }

        return shardStrategy;
    }


    private static String getModuleName(final YangInstanceIdentifier path) {
        String namespace = path.getPathArguments().iterator().next().getNodeType().getNamespace().toASCIIString();

        Optional<String> optional =
            configuration.getModuleNameFromNameSpace(namespace);

        if(!optional.isPresent()){
            return UNKNOWN_MODULE_NAME;
        }

        return optional.get();
    }
}
