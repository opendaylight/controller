/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MockConfiguration implements Configuration{
    @Override public List<String> getMemberShardNames(String memberName) {
        List<String> shardNames = new ArrayList<>();
        shardNames.add("default");
        return shardNames;
    }

    @Override public Optional<String> getModuleNameFromNameSpace(
        String nameSpace) {
        return Optional.absent();
    }

    @Override
    public Map<String, ShardStrategy> getModuleNameToShardStrategyMap() {
        return Collections.EMPTY_MAP;
    }

    @Override public List<String> getShardNamesFromModuleName(
        String moduleName) {
        return Collections.EMPTY_LIST;
    }
}
