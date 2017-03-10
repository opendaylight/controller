/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.testing.FakeTicker;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class AveragingProgressTrackerTest {
    private static final long CHECKER = TimeUnit.MILLISECONDS.toNanos(500);

    private static FakeTicker ticker;
    private static Random random;

    private AveragingProgressTracker averagingProgressTracker;

    @Before
    public void setUp() {
        ticker = new FakeTicker();
        random = new Random();

        averagingProgressTracker = new AveragingProgressTracker(4);

        long delay = averagingProgressTracker.estimateIsolatedDelay(moveTicker());
        assertEquals(0, delay);

        for (int i = 0; i < 2; i++) {
            delay = averagingProgressTracker.openTask(moveTicker());
            assertEquals(0, delay);
        }
    }

    @Test
    public void estimateIsolatedDelayTest() {
        // checker reached
        long delay = averagingProgressTracker.openTask(moveTicker());
        assertEquals(CHECKER, delay);

        // delay can be more than checker if number of tasks is growing
        delay = averagingProgressTracker.openTask(moveTicker());
        assertTrue(delay > CHECKER);

        // estimate can be max value of CHECKER
        delay = averagingProgressTracker.estimateIsolatedDelay(moveTicker());
        assertEquals(CHECKER, delay);

        // close one task
        averagingProgressTracker.closeTask(moveTicker(), 0, 0, 0);

        // number of tasks is less than half of limit, so delay is zero
        delay = averagingProgressTracker.estimateIsolatedDelay(moveTicker());
        assertEquals(0, delay);
    }

    @Test
    public void copyObjectTest() {
        final ProgressTracker copyAverageProgressTracker = new AveragingProgressTracker(averagingProgressTracker);
        assertEquals(copyAverageProgressTracker.openTask(moveTicker()),
                averagingProgressTracker.openTask(moveTicker()));
        assertEquals(averagingProgressTracker.tasksClosed(), copyAverageProgressTracker.tasksClosed());
        assertEquals(averagingProgressTracker.tasksEncountered(), copyAverageProgressTracker.tasksEncountered());
        assertEquals(averagingProgressTracker.tasksOpen(), copyAverageProgressTracker.tasksOpen());
    }

    private static long moveTicker() {
        final int advance = random.nextInt(Integer.MAX_VALUE) + 1;
        return ticker.advance(advance).read();
    }
}