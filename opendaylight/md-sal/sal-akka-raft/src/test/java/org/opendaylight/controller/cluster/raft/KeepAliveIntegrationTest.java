/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import scala.concurrent.duration.FiniteDuration;

/**
 * Tests KeepAlive end-to-end.
 *
 * @author Thomas Pantelis
 */
public class KeepAliveIntegrationTest extends AbstractRaftActorIntegrationTest {

    @Test
    public void testKeepAlive() {
        testLog.info("testKeepAlive starting");

        leaderConfigParams = newLeaderConfigParams();

        // Election timeout will be 300 ms and keep alive interval 60 ms.
        leaderConfigParams.setHeartBeatInterval(new FiniteDuration(50, TimeUnit.MILLISECONDS));
        leaderConfigParams.setElectionTimeoutFactor(6);

        leaderActor = newTestRaftActor(leaderId, ImmutableMap.of(follower1Id, testActorPath(follower1Id)),
                leaderConfigParams);
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderActor.underlyingActor().startDropMessages(RequestVote.class);

        DefaultConfigParamsImpl followerConfigParams = newFollowerConfigParams();
        followerConfigParams.setHeartBeatInterval(leaderConfigParams.getHeartBeatInterval());
        followerConfigParams.setElectionTimeoutFactor(leaderConfigParams.getElectionTimeoutFactor());
        follower1Actor = newTestRaftActor(follower1Id, ImmutableMap.of(leaderId, leaderActor.path().toString()),
                followerConfigParams);
        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();

        // Drop ElectionTimeout to the follower to ensure the KeepAlive prevents an election.
        follower1Actor.underlyingActor().startDropMessages(ElectionTimeout.class);

        waitUntilLeader(leaderActor);

        follower1Actor.underlyingActor().stopDropMessages(ElectionTimeout.class);

        // Wait for the first AppendEntries which will restart the election timer.
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);

        MessageCollectorActor.clearMessages(follower1CollectorActor);
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        // Send a Runnable message that sleeps for 2 * election timeout interval to block the leader actor
        // from sending AppendEntries.
        final long delay = followerConfigParams.getElectionTimeOutInterval().toMillis() * 2;
        leaderActor.tell(new Runnable() {
            @Override
            public void run() {
                testLog.info("Blocking the leader actor for {} ms", delay);
                Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
            }
        }, ActorRef.noSender());

        // Verify no ElectionT occurs within 2 * election timeout interval.
        MessageCollectorActor.assertNoneMatching(follower1CollectorActor, ElectionTimeout.class, delay);

        testLog.info("testKeepAlive ending");
    }
}
