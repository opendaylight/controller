/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.controller.cluster.datastore.ShardManager;
import org.opendaylight.controller.cluster.datastore.messages.SwitchShardBehavior;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardManagerInfo extends AbstractMXBean implements ShardManagerInfoMBean {

    public static String JMX_CATEGORY_SHARD_MANAGER = "ShardManager";

    // The only states that you can switch to from outside. You cannot switch to Candidate/IsolatedLeader for example
    private static final List<String> ACCEPTABLE_STATES
            = Lists.newArrayList(RaftState.Leader.name(), RaftState.Follower.name());

    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerInfo.class);

    private final List<String> localShards;

    private boolean syncStatus = false;

    private ShardManager shardManager;

    public ShardManagerInfo(String name, String mxBeanType, List<String> localShards) {
        super(name, mxBeanType, JMX_CATEGORY_SHARD_MANAGER);
        this.localShards = localShards;
    }

    public static ShardManagerInfo createShardManagerMBean(String name, String mxBeanType,
            List<String> localShards){
        ShardManagerInfo shardManagerInfo = new ShardManagerInfo(name, mxBeanType, localShards);

        shardManagerInfo.registerMBean();

        return shardManagerInfo;
    }

    @Override
    public List<String> getLocalShards() {
        return localShards;
    }

    @Override
    public boolean getSyncStatus() {
        return this.syncStatus;
    }

    @Override
    public void switchAllLocalShardsState(String newState, long term) {
        LOG.info("switchAllLocalShardsState called newState = {}, term = {}", newState, term);

        for(String shardName : localShards){
            switchShardState(shardName, newState, term);
        }
    }

    @Override
    public void switchShardState(String shardName, String newState, long term) {
        LOG.info("switchShardState called shardName = {}, newState = {}, term = {}", shardName, newState, term);

        Preconditions.checkArgument(localShards.contains(shardName), shardName + " is not local");
        Preconditions.checkArgument(ACCEPTABLE_STATES.contains(newState));

        shardManager.getSelf().tell(new SwitchShardBehavior(shardName, newState, term), ActorRef.noSender());
    }

    public void setSyncStatus(boolean syncStatus){
        this.syncStatus = syncStatus;
    }

    public void setShardManager(ShardManager shardManager){
        this.shardManager = shardManager;
    }
}
