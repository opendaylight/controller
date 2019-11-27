/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.ModuleConfig;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

public class MockConfiguration extends ConfigurationImpl {
    public MockConfiguration() {
        this(Collections.singletonMap("default", Arrays.asList("member-1", "member-2")));
    }

    public MockConfiguration(final Map<String, List<String>> shardMembers) {
        super(configuration -> {
            Map<String, ModuleConfig.Builder> retMap = new HashMap<>();
            for (Map.Entry<String, List<String>> e : shardMembers.entrySet()) {
                String shardName = e.getKey();
                retMap.put(shardName,
                    ModuleConfig.builder(shardName).shardConfig(
                        shardName, null,
                            e.getValue().stream().map(MemberName::forName).collect(Collectors.toList())));
            }

            return retMap;
        });
    }

    @Override
    public ShardStrategy getStrategyForPrefix(final DOMDataTreeIdentifier prefix) {
        return null;
    }
}
