/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.longThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;

/**
 * Unit tests for TransactionRateLimitingCallback.
 *
 * @author Thomas Pantelis
 */
public class TransactionRateLimitingCallbackTest {

    @Mock
    ActorContext mockContext;

    @Mock
    Timer mockTimer;

    TransactionRateLimitingCallback callback;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        doReturn(mockTimer).when(mockContext).getOperationTimer(ActorContext.COMMIT);
        callback = new TransactionRateLimitingCallback(mockContext);
    }

    @Test
    public void testSuccessWithoutPause() {
        callback.run();

        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

        callback.success();
        verify(mockTimer).update(inRange(TimeUnit.MILLISECONDS.toNanos(200),
                TimeUnit.MILLISECONDS.toNanos(1000)), eq(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testSuccessPause() {
        callback.run();

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        callback.pause();

        Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);

        callback.resume();

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        callback.success();
        verify(mockTimer).update(inRange(TimeUnit.MILLISECONDS.toNanos(200),
                TimeUnit.MILLISECONDS.toNanos(1000)), eq(TimeUnit.NANOSECONDS));
    }

    private Long inRange(final long min, final long max) {
        ArgumentMatcher<Long> matcher = new ArgumentMatcher<Long>() {
            @Override
            public boolean matches(Object argument) {
                Long other = (Long)argument;
                return other >= min && other < max;
            }
        };

        return longThat(matcher);
    }
}
