/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;


import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FollowerLogInformationImplTest {

    @Test
    public void testIsFollowerActive() {
        FollowerLogInformation followerLogInformation =
            new FollowerLogInformationImpl(
                "follower1", new AtomicLong(10), new AtomicLong(9));

        FiniteDuration timeoutDuration =
            new FiniteDuration(500, TimeUnit.MILLISECONDS);

        assertFalse("Follower should be termed inactive before stopwatch starts",
            followerLogInformation.isFollowerActive(timeoutDuration));

        followerLogInformation.markFollowerActive();
        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        assertTrue("Follower should be active", followerLogInformation.isFollowerActive(timeoutDuration));

        Uninterruptibles.sleepUninterruptibly(400, TimeUnit.MILLISECONDS);
        assertFalse("Follower should be inactive after time lapsed", followerLogInformation.isFollowerActive(timeoutDuration));

        followerLogInformation.markFollowerActive();
        assertTrue("Follower should be active from inactive", followerLogInformation.isFollowerActive(timeoutDuration));
    }
}
