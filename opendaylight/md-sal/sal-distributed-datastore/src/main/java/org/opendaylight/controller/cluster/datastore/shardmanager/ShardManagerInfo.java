/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Throwables;
import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior.BecomeFollower;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior.BecomeLeader;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.raft.api.RaftRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

final class ShardManagerInfo extends AbstractMXBean implements ShardManagerInfoMBean {

    public static final String JMX_CATEGORY_SHARD_MANAGER = "ShardManager";

    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerInfo.class);
    private static final long ASK_TIMEOUT_MILLIS = 5000;

    private final ActorRef shardManager;
    private final MemberName memberName;

    private volatile boolean syncStatus = false;


    ShardManagerInfo(final ActorRef shardManager, final MemberName memberName, final String name,
        final String mxBeanType) {
        super(name, mxBeanType, JMX_CATEGORY_SHARD_MANAGER);
        this.shardManager = requireNonNull(shardManager);
        this.memberName = requireNonNull(memberName);
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch"})
    @Override
    public List<String> getLocalShards() {
        try {
            return (List<String>) Await.result(
                Patterns.ask(shardManager, GetLocalShardIds.INSTANCE, ASK_TIMEOUT_MILLIS), Duration.Inf());
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean getSyncStatus() {
        return syncStatus;
    }

    void setSyncStatus(final boolean syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public String getMemberName() {
        return memberName.getName();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void requestSwitchShardState(final ShardIdentifier shardId, final String newState, final long term) {
        // Validates strings argument
        final var state = RaftRole.valueOf(newState);

        // Leader and Follower are the only states to which we can switch externally
        final var behavior = switch (state) {
            case Follower -> new BecomeFollower(term);
            case Leader -> new BecomeLeader(term);
            default -> throw new IllegalArgumentException("Illegal target state " + state);
        };

        final var future = Patterns.ask(shardManager, new SwitchShardBehavior(shardId, behavior), ASK_TIMEOUT_MILLIS);
        try {
            Await.result(future, Duration.Inf());
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void switchAllLocalShardsState(final String newState, final long term) {
        LOG.info("switchAllLocalShardsState called newState = {}, term = {}", newState, term);
        requestSwitchShardState(null, newState, term);
    }

    @Override
    public void switchShardState(final String shardId, final String newState, final long term) {
        final ShardIdentifier identifier = ShardIdentifier.fromShardIdString(shardId);
        LOG.info("switchShardState called shardName = {}, newState = {}, term = {}", shardId, newState, term);
        requestSwitchShardState(identifier, newState, term);
    }
}
