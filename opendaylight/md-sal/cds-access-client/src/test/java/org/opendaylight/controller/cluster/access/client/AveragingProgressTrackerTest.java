/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.testing.FakeTicker;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

public class AveragingProgressTrackerTest {
    private static final long CHECKER = TimeUnit.MILLISECONDS.toNanos(500);

    private static FakeTicker ticker;
    private static Random random;

    private AveragingProgressTracker averagingProgressTracker;

    @Before
    public void setUp() {
        ticker = new FakeTicker();
        random = new Random();
        averagingProgressTracker = new AveragingProgressTracker(3);
    }

    @Test
    public void estimateIsolatedDelayTest() {
        long time = moveTicker();
        assertEquals(0, averagingProgressTracker.estimateIsolatedDelay(time));

        // less than half
        time = moveTicker();
        assertTrue(averagingProgressTracker.openTask(time) <= CHECKER);
        assertEquals(0, averagingProgressTracker.estimateIsolatedDelay(time));

        // more than half but less than limit
        time = moveTicker();
        assertTrue(averagingProgressTracker.openTask(time) <= CHECKER);
        assertTrue(averagingProgressTracker.estimateIsolatedDelay(time) < CHECKER);

        // limit reached
        time = moveTicker();
        assertTrue(averagingProgressTracker.openTask(time) >= CHECKER);
        assertEquals(CHECKER, averagingProgressTracker.estimateIsolatedDelay(time));

        // above limit, no higher isolated delays than CHECKER
        time = moveTicker();
        assertTrue(averagingProgressTracker.openTask(time) >= CHECKER);
        assertEquals(CHECKER, averagingProgressTracker.estimateIsolatedDelay(time));

        // close tasks to get under the half
        averagingProgressTracker.closeTask(moveTicker(), 0, 0, 0);
        averagingProgressTracker.closeTask(moveTicker(), 0, 0, 0);
        averagingProgressTracker.closeTask(moveTicker(), 0, 0, 0);

        assertEquals(0, averagingProgressTracker.estimateIsolatedDelay(moveTicker()));
    }

    @Test
    public void copyObjectTest() {
        final AveragingProgressTracker copyAverageProgressTracker = new AveragingProgressTracker(
                averagingProgressTracker);

        // copied object is the same as original
        assertTrue(new ReflectionEquals(averagingProgressTracker).matches(copyAverageProgressTracker));

        // afterwards work of copied tracker is independent
        averagingProgressTracker.openTask(moveTicker());

        final long time = moveTicker();
        assertNotEquals("Trackers are expected to return different results for tracking",
                averagingProgressTracker.openTask(time), copyAverageProgressTracker.openTask(time));
        assertNotEquals("Trackers are expected to encounter different amount of tasks",
                averagingProgressTracker.tasksEncountered(), copyAverageProgressTracker.tasksEncountered());

        // and copied object is then no more the same as original
        assertFalse(new ReflectionEquals(averagingProgressTracker).matches(copyAverageProgressTracker));
    }

    private static long moveTicker() {
        final int advance = random.nextInt(Integer.MAX_VALUE) + 1;
        return ticker.advance(advance).read();
    }
}