/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

public class FollowerLogInformationImplTest {

    @Test
    public void testIsFollowerActive() {

        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(10);

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(500, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1);
        context.setConfigParams(configParams);

        FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(new PeerInfo("follower1", null, VotingState.VOTING), 9, context);

        assertFalse("Follower should be termed inactive before stopwatch starts",
                followerLogInformation.isFollowerActive());

        followerLogInformation.markFollowerActive();
        if (sleepWithElaspsedTimeReturned(200) > 200) {
            return;
        }
        assertTrue("Follower should be active", followerLogInformation.isFollowerActive());

        if (sleepWithElaspsedTimeReturned(400) > 400) {
            return;
        }
        assertFalse("Follower should be inactive after time lapsed",
                followerLogInformation.isFollowerActive());

        followerLogInformation.markFollowerActive();
        assertTrue("Follower should be active from inactive",
                followerLogInformation.isFollowerActive());
    }

    // we cannot rely comfortably that the sleep will indeed sleep for the desired time
    // hence getting the actual elapsed time and do a match.
    // if the sleep has spilled over, then return the test gracefully
    private static long sleepWithElaspsedTimeReturned(long millis) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Uninterruptibles.sleepUninterruptibly(millis, TimeUnit.MILLISECONDS);
        stopwatch.stop();
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Test
    public void testOkToReplicate(){
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(0);
        FollowerLogInformation followerLogInformation =
                new FollowerLogInformationImpl(new PeerInfo("follower1", null, VotingState.VOTING), 10, context);

        assertTrue(followerLogInformation.okToReplicate());
        assertFalse(followerLogInformation.okToReplicate());

        // wait for 150 milliseconds and it should work again
        Uninterruptibles.sleepUninterruptibly(150, TimeUnit.MILLISECONDS);
        assertTrue(followerLogInformation.okToReplicate());

        //increment next index and try immediately and it should work again
        followerLogInformation.incrNextIndex();
        assertTrue(followerLogInformation.okToReplicate());
    }

    @Test
    public void testVotingNotInitializedState() {
        final PeerInfo peerInfo = new PeerInfo("follower1", null, VotingState.VOTING_NOT_INITIALIZED);
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(0);
        FollowerLogInformation followerLogInformation = new FollowerLogInformationImpl(peerInfo, -1, context);

        assertFalse(followerLogInformation.okToReplicate());

        followerLogInformation.markFollowerActive();
        assertFalse(followerLogInformation.isFollowerActive());

        peerInfo.setVotingState(VotingState.VOTING);
        assertTrue(followerLogInformation.okToReplicate());

        followerLogInformation.markFollowerActive();
        assertTrue(followerLogInformation.isFollowerActive());
    }

    @Test
    public void testNonVotingState() {
        final PeerInfo peerInfo = new PeerInfo("follower1", null, VotingState.NON_VOTING);
        MockRaftActorContext context = new MockRaftActorContext();
        context.setCommitIndex(0);
        FollowerLogInformation followerLogInformation = new FollowerLogInformationImpl(peerInfo, -1, context);

        assertTrue(followerLogInformation.okToReplicate());

        followerLogInformation.markFollowerActive();
        assertTrue(followerLogInformation.isFollowerActive());
    }
}
