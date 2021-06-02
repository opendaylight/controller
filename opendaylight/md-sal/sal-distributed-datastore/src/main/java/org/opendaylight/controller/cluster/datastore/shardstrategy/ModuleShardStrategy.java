/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ModuleShardStrategy implements ShardStrategy {

    public static final String NAME = "module";

    private final String moduleName;
    private final Configuration configuration;

    public ModuleShardStrategy(String moduleName, Configuration configuration) {
        this.moduleName = moduleName;
        this.configuration = configuration;
    }

    @Override
    public String findShard(YangInstanceIdentifier path) {
        String shardName = configuration.getShardNameForModule(moduleName);
        return shardName != null ? shardName : DefaultShardStrategy.DEFAULT_SHARD;
    }
}
