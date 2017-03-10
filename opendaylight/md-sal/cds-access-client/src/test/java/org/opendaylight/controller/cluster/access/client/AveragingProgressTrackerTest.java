/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class AveragingProgressTrackerTest {

    private static final long CHECKER = TimeUnit.MILLISECONDS.toNanos(500);
    
    private static final long NOW = TimeUnit.MILLISECONDS.toNanos(1000);
    private ProgressTracker averagingProgressTracker;

    @Before
    public void setUp() {
        averagingProgressTracker = new AveragingProgressTracker(4);
        long delay = averagingProgressTracker.estimateIsolatedDelay(computeTime());
        assertEquals(0, delay);
        for (int i = 0; i < 2; i++) {
            delay = averagingProgressTracker.openTask(computeTime());
            assertEquals(0, delay);
        }
    }

    @Test
    public void estimateIsolatedDelayTest() {
        long delay = averagingProgressTracker.openTask(computeTime());
        assertEquals(CHECKER, delay);

        delay = averagingProgressTracker.openTask(computeTime());
        assertEquals(CHECKER, delay);

        delay = averagingProgressTracker.estimateIsolatedDelay(computeTime());
        assertEquals(CHECKER, delay);

        averagingProgressTracker.closeTask(3000000000L, 0, 0, 0);

        delay = averagingProgressTracker.estimateIsolatedDelay(computeTime());
        assertEquals(0, delay);
    }

    @Test
    public void copyObjectTest() {
        final ProgressTracker copyAverageProgressTracker =
                new AveragingProgressTracker((AveragingProgressTracker) averagingProgressTracker);
        assertEquals(copyAverageProgressTracker.openTask(computeTime()), averagingProgressTracker.openTask(computeTime()));
        assertEquals(averagingProgressTracker.tasksClosed(), copyAverageProgressTracker.tasksClosed());
        assertEquals(averagingProgressTracker.tasksEncountered(), copyAverageProgressTracker.tasksEncountered());
        assertEquals(averagingProgressTracker.tasksOpen(), copyAverageProgressTracker.tasksOpen());
    }

    private long computeTime() {
        return TimeUnit.MILLISECONDS.toNanos(TimeUnit.NANOSECONDS.toMillis(NOW) + 100);
    }


}