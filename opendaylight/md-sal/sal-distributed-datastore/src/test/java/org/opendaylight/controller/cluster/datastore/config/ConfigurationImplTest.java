/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;

public class ConfigurationImplTest {

    private ConfigurationImpl configuration;

    @Before
    public void setup(){
        configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");
    }

    @Test
    public void testConstructor(){
        Assert.assertNotNull(configuration);
    }

    @Test
    public void testGetMemberShardNames(){
        Collection<String> memberShardNames = configuration.getMemberShardNames("member-1");
        assertEquals("getMemberShardNames", ImmutableSortedSet.of("people-1", "cars-1", "test-1", "default"),
                ImmutableSortedSet.copyOf(memberShardNames));

        memberShardNames = configuration.getMemberShardNames("member-2");
        assertEquals("getMemberShardNames", ImmutableSortedSet.of("default"),
                ImmutableSortedSet.copyOf(memberShardNames));

        memberShardNames = configuration.getMemberShardNames("member-100");
        assertEquals("getMemberShardNames size", 0, memberShardNames.size());
    }

    @Test
    public void testGetMembersFromShardName(){
        Collection<String> members = configuration.getMembersFromShardName("default");
        assertEquals("getMembersFromShardName", ImmutableSortedSet.of("member-1", "member-2", "member-3"),
                ImmutableSortedSet.copyOf(members));

        members = configuration.getMembersFromShardName("cars-1");
        assertEquals("getMembersFromShardName", ImmutableSortedSet.of("member-1"),
                ImmutableSortedSet.copyOf(members));

        // Try to find a shard which is not present

        members = configuration.getMembersFromShardName("foobar");
        assertEquals("getMembersFromShardName size", 0, members.size());
    }

    @Test
    public void testGetAllShardNames(){
        Set<String> allShardNames = configuration.getAllShardNames();
        assertEquals("getAllShardNames", ImmutableSortedSet.of("people-1", "cars-1", "test-1", "default"),
                ImmutableSortedSet.copyOf(allShardNames));
    }

    @Test
    public void testGetModuleNameFromNameSpace() {
        String moduleName = configuration.getModuleNameFromNameSpace(
                "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test:cars");
        assertEquals("getModuleNameFromNameSpace", "cars", moduleName);

        moduleName = configuration.getModuleNameFromNameSpace(
                "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test");
        assertEquals("getModuleNameFromNameSpace", "test", moduleName);

        moduleName = configuration.getModuleNameFromNameSpace("non-existent");
        assertNull("getModuleNameFromNameSpace - expected null", moduleName);
    }

    @Test
    public void testGetStrategyForModule() {
        ShardStrategy strategy = configuration.getStrategyForModule("cars");
        assertNotNull("getStrategyForModule null", strategy);
        assertEquals("getStrategyForModule type", ModuleShardStrategy.class, strategy.getClass());

        strategy = configuration.getStrategyForModule("people");
        assertNotNull("getStrategyForModule null", strategy);
        assertEquals("getStrategyForModule type", ModuleShardStrategy.class, strategy.getClass());

        strategy = configuration.getStrategyForModule("default");
        assertNull("getStrategyForModule - expected null", strategy);

        strategy = configuration.getStrategyForModule("non-existent");
        assertNull("getStrategyForModule - expected null", strategy);
    }

    @Test
    public void testGetShardNameForModule() {
        String shardName = configuration.getShardNameForModule("cars");
        assertEquals("getShardNameForModule", "cars-1", shardName);

        shardName = configuration.getShardNameForModule("people");
        assertEquals("getShardNameForModule", "people-1", shardName);

        shardName = configuration.getShardNameForModule("non-existent");
        assertNull("getShardNameForModule - expected null", shardName);
    }

    @Test
    public void testAddModuleShardConfiguration() throws Exception {
        URI namespace = new URI("urn:opendaylight:test:oven");
        String moduleName = "oven";
        String shardName = "oven-shard";
        String shardStrategyName = ModuleShardStrategy.NAME;
        Collection<String> shardMemberNames = ImmutableSortedSet.of("member-1", "member-4", "member-5");

        configuration.addModuleShardConfiguration(new ModuleShardConfiguration(namespace, moduleName, shardName,
                shardStrategyName, shardMemberNames));

        assertEquals("getMemberShardNames", ImmutableSortedSet.of("people-1", "cars-1", "test-1", "default", shardName),
                ImmutableSortedSet.copyOf(configuration.getMemberShardNames("member-1")));
        assertEquals("getMemberShardNames", ImmutableSortedSet.of(shardName),
                ImmutableSortedSet.copyOf(configuration.getMemberShardNames("member-4")));
        assertEquals("getMemberShardNames", ImmutableSortedSet.of(shardName),
                ImmutableSortedSet.copyOf(configuration.getMemberShardNames("member-5")));
        assertEquals("getMembersFromShardName", shardMemberNames,
                ImmutableSortedSet.copyOf(configuration.getMembersFromShardName(shardName)));
        assertEquals("getShardNameForModule", shardName, configuration.getShardNameForModule(moduleName));
        assertEquals("getModuleNameFromNameSpace", moduleName,
                configuration.getModuleNameFromNameSpace(namespace.toASCIIString()));
        assertEquals("getAllShardNames", ImmutableSortedSet.of("people-1", "cars-1", "test-1", "default", shardName),
                ImmutableSortedSet.copyOf(configuration.getAllShardNames()));

        ShardStrategy strategy = configuration.getStrategyForModule("cars");
        assertNotNull("getStrategyForModule null", strategy);
        assertEquals("getStrategyForModule type", ModuleShardStrategy.class, strategy.getClass());
    }

    @Test
    public void testGetUniqueMemberNamesForAllShards() {
        assertEquals("getUniqueMemberNamesForAllShards", Sets.newHashSet("member-1", "member-2", "member-3"),
                configuration.getUniqueMemberNamesForAllShards());
    }

    @Test
    public void testAddMemberReplicaForShard() {
        configuration.addMemberReplicaForShard("people-1", "member-2");
        String shardName = configuration.getShardNameForModule("people");
        assertEquals("ModuleShardName", "people-1", shardName);
        ShardStrategy shardStrategy = configuration.getStrategyForModule("people");
        assertEquals("ModuleStrategy", ModuleShardStrategy.class, shardStrategy.getClass());
        Collection<String> members = configuration.getMembersFromShardName("people-1");
        assertEquals("Members", ImmutableSortedSet.of("member-1", "member-2"),
            ImmutableSortedSet.copyOf(members));

        configuration.addMemberReplicaForShard("non-existent", "member-2");
        Set<String> shardNames = configuration.getAllShardNames();
        assertEquals("ShardNames", ImmutableSortedSet.of("people-1", "cars-1", "test-1", "default"),
            ImmutableSortedSet.copyOf(shardNames));
    }

    @Test
    public void testRemoveMemberReplicaForShard() {
        configuration.removeMemberReplicaForShard("default", "member-2");
        String shardName = configuration.getShardNameForModule("default");
        assertEquals("ModuleShardName", "default", shardName);
        ShardStrategy shardStrategy = configuration.getStrategyForModule("default");
        assertNull("ModuleStrategy", shardStrategy);
        Collection<String> members = configuration.getMembersFromShardName("default");
        assertEquals("Members", ImmutableSortedSet.of("member-1", "member-3"),
            ImmutableSortedSet.copyOf(members));

        configuration.removeMemberReplicaForShard("non-existent", "member-2");
        Set<String> shardNames = configuration.getAllShardNames();
        assertEquals("ShardNames", ImmutableSortedSet.of("people-1", "cars-1", "test-1", "default"),
            ImmutableSortedSet.copyOf(shardNames));
    }
}
