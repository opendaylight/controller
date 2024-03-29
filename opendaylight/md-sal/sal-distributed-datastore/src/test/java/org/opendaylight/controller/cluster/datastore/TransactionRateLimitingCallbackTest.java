/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Timer;
import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;

/**
 * Unit tests for TransactionRateLimitingCallback.
 *
 * @author Thomas Pantelis
 */
@Deprecated(since = "9.0.0", forRemoval = true)
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TransactionRateLimitingCallbackTest {
    @Mock
    ActorUtils mockContext;
    @Mock
    Timer mockTimer;
    @Mock
    Ticker mockTicker;

    TransactionRateLimitingCallback callback;

    @Before
    public void setUp() {
        doReturn(mockTimer).when(mockContext).getOperationTimer(ActorUtils.COMMIT);
        callback = new TransactionRateLimitingCallback(mockContext);
        TransactionRateLimitingCallback.setTicker(mockTicker);
    }

    @Test
    public void testSuccessWithoutPause() {
        doReturn(1L).doReturn(201L).when(mockTicker).read();

        callback.run();
        callback.success();

        verify(mockTimer).update(200L, TimeUnit.NANOSECONDS);
    }

    @Test
    public void testSuccessWithPause() {
        doReturn(1L).doReturn(201L).doReturn(301L).doReturn(351L).when(mockTicker).read();

        callback.run();
        callback.pause();
        callback.pause();
        callback.resume();
        callback.resume();
        callback.success();

        verify(mockTimer).update(250L, TimeUnit.NANOSECONDS);
    }

    @Test
    public void testFailure() {
        doReturn(1L).when(mockTicker).read();

        callback.run();
        callback.failure();

        verify(mockTimer, never()).update(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testSuccessWithoutRun() {
        final var ex = assertThrows(IllegalStateException.class, callback::success);

        verify(mockTimer, never()).update(anyLong(), any(TimeUnit.class));
    }
}
