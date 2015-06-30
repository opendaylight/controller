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
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

public class FollowerLogInformationImplTest {

    @Test
    public void testIsFollowerActive() {

        MockRaftActorContext context = new MockRaftActorContext();

        FiniteDuration timeoutDuration =
            new FiniteDuration(500, TimeUnit.MILLISECONDS);

        FollowerLogInformation followerLogInformation =
            new FollowerLogInformationImpl(
                "follower1", new AtomicLong(10), new AtomicLong(9), timeoutDuration, context);

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
    private long sleepWithElaspsedTimeReturned(long millis) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        Uninterruptibles.sleepUninterruptibly(millis, TimeUnit.MILLISECONDS);
        stopwatch.stop();
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Test
    public void testOkToReplicate(){
        MockRaftActorContext context = new MockRaftActorContext();
        context.setConfigParams(new DefaultConfigParamsImpl());
        context.setCommitIndex(9);
        FollowerLogInformation followerLogInformation = new FollowerLogInformationImpl("follower1",
                new AtomicLong(10), new AtomicLong(9), new FiniteDuration(500, TimeUnit.MILLISECONDS), context);

        assertTrue(followerLogInformation.okToReplicate());
        assertFalse(followerLogInformation.okToReplicate());

        // wait for 150 milliseconds and it should work again
        Uninterruptibles.sleepUninterruptibly(150, TimeUnit.MILLISECONDS);
        assertTrue(followerLogInformation.okToReplicate());

        //increment next index and try immediately and it should work again
        followerLogInformation.incrNextIndex();
        assertTrue(followerLogInformation.okToReplicate());
    }
}
