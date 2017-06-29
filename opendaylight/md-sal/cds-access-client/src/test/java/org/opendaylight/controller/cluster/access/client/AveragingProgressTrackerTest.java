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
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class AveragingProgressTrackerTest {
    private static final long CHECKER = TimeUnit.MILLISECONDS.toNanos(500);
    private static final long TICKER_STEP = 100;

    private static FakeTicker ticker;

    private AveragingProgressTracker averagingProgressTracker;

    @Before
    public void setUp() {
        ticker = new FakeTicker();
        ticker.setAutoIncrementStep(TICKER_STEP, TimeUnit.MILLISECONDS);
        averagingProgressTracker = new AveragingProgressTracker(3);
    }

    @Test
    public void estimateIsolatedDelayTest() {
        long time = ticker.read();
        assertEquals(0, averagingProgressTracker.estimateIsolatedDelay(time));

        // less than half
        time = ticker.read();
        assertTrue(averagingProgressTracker.openTask(time) <= CHECKER);
        assertEquals(0, averagingProgressTracker.estimateIsolatedDelay(time));

        // more than half but less than limit
        time = ticker.read();
        assertTrue(averagingProgressTracker.openTask(time) <= CHECKER);
        assertTrue(averagingProgressTracker.estimateIsolatedDelay(time) < CHECKER);

        // limit reached
        time = ticker.read();
        assertTrue(averagingProgressTracker.openTask(time) >= CHECKER);
        assertEquals(CHECKER, averagingProgressTracker.estimateIsolatedDelay(time));

        // above limit, no higher isolated delays than CHECKER
        time = ticker.read();
        assertTrue(averagingProgressTracker.openTask(time) >= CHECKER);
        assertEquals(CHECKER, averagingProgressTracker.estimateIsolatedDelay(time));

        // close tasks to get under the half
        averagingProgressTracker.closeTask(ticker.read(), 0, 0, 0);
        averagingProgressTracker.closeTask(ticker.read(), 0, 0, 0);
        averagingProgressTracker.closeTask(ticker.read(), 0, 0, 0);

        assertEquals(0, averagingProgressTracker.estimateIsolatedDelay(ticker.read()));
    }

}