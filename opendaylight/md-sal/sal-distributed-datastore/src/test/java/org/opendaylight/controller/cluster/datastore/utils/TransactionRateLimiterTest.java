/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;

// FIXME: use Strict runner
@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionRateLimiterTest {
    @Mock
    public ActorUtils actorUtils;
    @Mock
    public DatastoreContext datastoreContext;
    @Mock
    public Timer commitTimer;
    @Mock
    private Timer.Context commitTimerContext;
    @Mock
    private Snapshot commitSnapshot;

    @Before
    public void setUp() {
        doReturn(datastoreContext).when(actorUtils).getDatastoreContext();
        doReturn(30).when(datastoreContext).getShardTransactionCommitTimeoutInSeconds();
        doReturn(100L).when(datastoreContext).getTransactionCreationInitialRateLimit();
        doReturn(commitTimer).when(actorUtils).getOperationTimer("commit");
        doReturn(commitTimerContext).when(commitTimer).time();
        doReturn(commitSnapshot).when(commitTimer).getSnapshot();
    }

    @Test
    public void testAcquireRateLimitChanged() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(292));
        assertEquals(147, rateLimiter.getPollOnCount());
    }

    @Test
    public void testAcquirePercentileValueZero() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        doReturn(TimeUnit.MILLISECONDS.toNanos(0) * 1D).when(commitSnapshot).getValue(0.1);

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(192));
        assertEquals(97, rateLimiter.getPollOnCount());
    }

    @Test
    public void testAcquireOnePercentileValueVeryHigh() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        // ten seconds
        doReturn(TimeUnit.MILLISECONDS.toNanos(10000) * 1D).when(commitSnapshot).getValue(1.0);

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(282));
        assertEquals(142, rateLimiter.getPollOnCount());
    }

    @Test
    public void testAcquireWithAllPercentileValueVeryHigh() {

        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(10000) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        // The initial rate limit will be retained here because the calculated rate limit was too small
        assertThat(rateLimiter.getTxCreationLimit(), approximately(100));
        assertEquals(1, rateLimiter.getPollOnCount());
    }

    @Test
    public void testAcquireWithRealPercentileValues() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(8) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        doReturn(TimeUnit.MILLISECONDS.toNanos(20) * 1D).when(commitSnapshot).getValue(0.7);
        doReturn(TimeUnit.MILLISECONDS.toNanos(100) * 1D).when(commitSnapshot).getValue(0.9);
        doReturn(TimeUnit.MILLISECONDS.toNanos(200) * 1D).when(commitSnapshot).getValue(1.0);

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(101));
        assertEquals(51, rateLimiter.getPollOnCount());
    }

    @Test
    public void testAcquireGetRateLimitFromOtherDataStores() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(0.0D).when(commitSnapshot).getValue(i * 0.1);
        }

        Timer operationalCommitTimer = mock(Timer.class);
        Timer.Context operationalCommitTimerContext = mock(Timer.Context.class);
        Snapshot operationalCommitSnapshot = mock(Snapshot.class);

        doReturn(operationalCommitTimer).when(actorUtils).getOperationTimer("operational", "commit");
        doReturn(operationalCommitTimerContext).when(operationalCommitTimer).time();
        doReturn(operationalCommitSnapshot).when(operationalCommitTimer).getSnapshot();

        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(i) * 1D).when(operationalCommitSnapshot).getValue(i * 0.1);
        }


        DatastoreContext.getGlobalDatastoreNames().add("config");
        DatastoreContext.getGlobalDatastoreNames().add("operational");

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(292));
        assertEquals(147, rateLimiter.getPollOnCount());
    }

    @Test
    public void testRateLimiting() {
        for (int i = 1; i < 11; i++) {
            doReturn(TimeUnit.SECONDS.toNanos(1) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);

        Stopwatch watch = Stopwatch.createStarted();

        rateLimiter.acquire();
        rateLimiter.acquire();
        rateLimiter.acquire();

        watch.stop();

        assertThat("did not take as much time as expected rate limit : " + rateLimiter.getTxCreationLimit(),
            watch.elapsed(), greaterThan(Duration.ofSeconds(1)));
    }

    @Test
    public void testRateLimitNotCalculatedUntilPollCountReached() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(8) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        doReturn(TimeUnit.MILLISECONDS.toNanos(20) * 1D).when(commitSnapshot).getValue(0.7);
        doReturn(TimeUnit.MILLISECONDS.toNanos(100) * 1D).when(commitSnapshot).getValue(0.9);
        doReturn(TimeUnit.MILLISECONDS.toNanos(200) * 1D).when(commitSnapshot).getValue(1.0);

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(101));
        assertEquals(51, rateLimiter.getPollOnCount());

        for (int i = 0; i < 49; i++) {
            rateLimiter.acquire();
        }

        verify(commitTimer, times(1)).getSnapshot();

        // Acquiring one more time will cause the re-calculation of the rate limit
        rateLimiter.acquire();

        verify(commitTimer, times(2)).getSnapshot();
    }

    @Test
    public void testAcquireNegativeAcquireAndPollOnCount() {
        for (int i = 1; i < 11; i++) {
            // Keep on increasing the amount of time it takes to complete transaction for each tenth of a
            // percentile. Essentially this would be 1ms for the 10th percentile, 2ms for 20th percentile and so on.
            doReturn(TimeUnit.MILLISECONDS.toNanos(8) * 1D).when(commitSnapshot).getValue(i * 0.1);
        }

        doReturn(TimeUnit.MILLISECONDS.toNanos(20) * 1D).when(commitSnapshot).getValue(0.7);
        doReturn(TimeUnit.MILLISECONDS.toNanos(100) * 1D).when(commitSnapshot).getValue(0.9);
        doReturn(TimeUnit.MILLISECONDS.toNanos(200) * 1D).when(commitSnapshot).getValue(1.0);

        TransactionRateLimiter rateLimiter = new TransactionRateLimiter(actorUtils);
        rateLimiter.setAcquireCount(Long.MAX_VALUE - 1);
        rateLimiter.setPollOnCount(Long.MAX_VALUE);
        rateLimiter.acquire();

        assertThat(rateLimiter.getTxCreationLimit(), approximately(101));
        assertEquals(-9223372036854775759L, rateLimiter.getPollOnCount());

        for (int i = 0; i < 50; i++) {
            rateLimiter.acquire();
        }

        verify(commitTimer, times(2)).getSnapshot();
    }

    public Matcher<Double> approximately(final double val) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(final Object obj) {
                Double value = (Double) obj;
                return value >= val && value <= val + 1;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("> " + val + " < " + (val + 1));
            }
        };
    }
}
