/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

public class MockConfiguration implements Configuration{
    private Map<String, List<String>> shardMembers = ImmutableMap.<String, List<String>>builder().
            put("default", Arrays.asList("member-1", "member-2")).
            /*put("astronauts", Arrays.asList("member-2", "member-3")).*/build();

    public MockConfiguration() {
    }

    public MockConfiguration(Map<String, List<String>> shardMembers) {
        this.shardMembers = shardMembers;
    }

    @Override
    public Collection<String> getMemberShardNames(final String memberName) {
        return new ArrayList<>(shardMembers.keySet());
    }

    @Override
    public String getModuleNameFromNameSpace(final String nameSpace) {
        return null;
    }

    @Override
    public String getShardNameForModule(final String moduleName) {
        return null;
    }

    @Override
    public Collection<String> getMembersFromShardName(final String shardName) {
        if("default".equals(shardName)) {
            return Arrays.asList("member-1", "member-2");
        } else if("astronauts".equals(shardName)){
            return Arrays.asList("member-2", "member-3");
        }

        List<String> members = shardMembers.get(shardName);
        return members != null ? members : Collections.<String>emptyList();
    }

    @Override public Set<String> getAllShardNames() {
        return Collections.emptySet();
    }

    @Override
    public Collection<String> getUniqueMemberNamesForAllShards() {
        Set<String> allNames = new HashSet<>();
        for(List<String> l: shardMembers.values()) {
            allNames.addAll(l);
        }

        return allNames;
    }

    @Override
    public ShardStrategy getStrategyForModule(String moduleName) {
        return null;
    }

    @Override
    public void addModuleShardConfiguration(ModuleShardConfiguration config) {
    }
    @Override
    public boolean checkModuleAvailability(String moduleName) {
        return false;
    }
}
