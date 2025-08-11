/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.testing.FakeTicker;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageTrackerTest {
    private static final class Foo {
        // Intentionally empty
    }

    private static final Logger LOG = LoggerFactory.getLogger(MessageTrackerTest.class);

    private FakeTicker ticker;
    private MessageTracker messageTracker;

    @Before
    public void setup() {
        ticker = new FakeTicker();
        messageTracker = new MessageTracker(Foo.class, 10, ticker);
    }

    @Test
    public void testNoTracking() {
        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        ticker.advance(20, MILLISECONDS);
        MessageTracker.Context context2 = messageTracker.received(new Foo());
        context2.close();
    }

    @Test
    public void testFailedExpectationOnTracking() {
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        ticker.advance(20, MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        final var error = context2.error();
        assertNotNull(error);
        assertEquals(List.of(), error.getMessageProcessingTimesSinceLastExpectedMessage());
    }

    @Test
    public void testFailedExpectationOnTrackingWithMessagesInBetween() {
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        messageTracker.received("A").close();
        messageTracker.received(10L).close();
        MessageTracker.Context context = messageTracker.received(100);

        ticker.advance(20, MILLISECONDS);

        context.close();

        MessageTracker.Context context2 = messageTracker.received(new Foo());


        MessageTracker.Error error = context2.error();
        assertNotNull(error);

        List<MessageTracker.MessageProcessingTime> messageProcessingTimes =
                error.getMessageProcessingTimesSinceLastExpectedMessage();

        assertEquals(3, messageProcessingTimes.size());

        assertEquals(String.class, messageProcessingTimes.get(0).getMessageClass());
        assertEquals(Long.class, messageProcessingTimes.get(1).getMessageClass());
        assertEquals(Integer.class, messageProcessingTimes.get(2).getMessageClass());
        assertTrue(messageProcessingTimes.get(2).getElapsedTimeInNanos() > MILLISECONDS.toNanos(10));
        assertEquals(Foo.class, error.getLastExpectedMessage().getClass());
        assertEquals(Foo.class, error.getCurrentExpectedMessage().getClass());

        LOG.error("An error occurred : {}" , error);
    }

    @Test
    public void testMetExpectationOnTracking() {
        messageTracker.begin();

        try (var context1 = messageTracker.received(new Foo())) {
            // nothing else
        }

        ticker.advance(1, MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        assertNull(context2.error());
    }

    @Test
    public void testIllegalStateExceptionWhenDoneIsNotCalledWhileTracking() {
        messageTracker.begin();

        messageTracker.received(new Foo());

        assertThrows(IllegalStateException.class, () -> messageTracker.received(new Foo()));
    }

    @Test
    public void testNoIllegalStateExceptionWhenDoneIsNotCalledWhileNotTracking() {
        messageTracker.received(new Foo());
        messageTracker.received(new Foo());
    }

    @Test
    public void testDelayInFirstExpectedMessageArrival() {
        messageTracker.begin();

        ticker.advance(20, MILLISECONDS);

        MessageTracker.Context context = messageTracker.received(new Foo());

        MessageTracker.Error error = context.error();
        assertNotNull(error);

        assertEquals(null, error.getLastExpectedMessage());
        assertEquals(Foo.class, error.getCurrentExpectedMessage().getClass());

        String errorString = error.toString();
        assertTrue(errorString.contains("Last Expected Message = null"));

        LOG.error("An error occurred : {}", error);
    }

    @Test
    public void testCallingBeginDoesNotResetWatch() {
        messageTracker.begin();

        ticker.advance(20, MILLISECONDS);

        messageTracker.begin();

        MessageTracker.Context context = messageTracker.received(new Foo());
        MessageTracker.Error error = context.error();
        assertNotNull(error);
    }

    @Test
    public void testMessagesSinceLastExpectedMessage() {

        messageTracker.begin();

        try (MessageTracker.Context ctx = messageTracker.received(45)) {
            assertNull(ctx.error());
        }
        try (MessageTracker.Context ctx = messageTracker.received(45L)) {
            assertNull(ctx.error());
        }

        List<MessageTracker.MessageProcessingTime> processingTimeList =
                messageTracker.getMessagesSinceLastExpectedMessage();

        assertEquals(2, processingTimeList.size());

        assertEquals(Integer.class, processingTimeList.get(0).getMessageClass());
        assertEquals(Long.class, processingTimeList.get(1).getMessageClass());
    }
}
