/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageTrackerTest {
    private static final class Foo {
        // Intentionally empty
    }

    private final static Logger LOG = LoggerFactory.getLogger(MessageTrackerTest.class);

    private TestTicker ticker;
    private MessageTracker messageTracker;

    @Before
    public void setup() {
        ticker = new TestTicker();
        messageTracker = new MessageTracker(Foo.class, 10, ticker);
    }

    @Test
    public void testNoTracking() {
        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        ticker.increment(MILLISECONDS.toNanos(20));
        MessageTracker.Context context2 = messageTracker.received(new Foo());
        context2.close();
    }

    @Test
    public void testFailedExpectationOnTracking() {
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        ticker.increment(MILLISECONDS.toNanos(20));

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        Assert.assertEquals(true, context2.error().isPresent());
        Assert.assertEquals(0, context2.error().get().getMessageProcessingTimesSinceLastExpectedMessage().size());

    }

    @Test
    public void testFailedExpectationOnTrackingWithMessagesInBetween() {
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        messageTracker.received("A").close();
        messageTracker.received(10L).close();
        MessageTracker.Context c = messageTracker.received(100);

        ticker.increment(MILLISECONDS.toNanos(20));

        c.close();

        MessageTracker.Context context2 = messageTracker.received(new Foo());

        Assert.assertEquals(true, context2.error().isPresent());

        MessageTracker.Error error = context2.error().get();

        List<MessageTracker.MessageProcessingTime> messageProcessingTimes =
                error.getMessageProcessingTimesSinceLastExpectedMessage();

        Assert.assertEquals(3, messageProcessingTimes.size());

        Assert.assertEquals(String.class, messageProcessingTimes.get(0).getMessageClass());
        Assert.assertEquals(Long.class, messageProcessingTimes.get(1).getMessageClass());
        Assert.assertEquals(Integer.class, messageProcessingTimes.get(2).getMessageClass());
        Assert.assertTrue(messageProcessingTimes.get(2).getElapsedTimeInNanos() > MILLISECONDS.toNanos(10));
        Assert.assertEquals(Foo.class, error.getLastExpectedMessage().getClass());
        Assert.assertEquals(Foo.class, error.getCurrentExpectedMessage().getClass());

        LOG.error("An error occurred : {}" , error);
    }


    @Test
    public void testMetExpectationOnTracking() {
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.close();

        ticker.increment(MILLISECONDS.toNanos(1));

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        Assert.assertEquals(false, context2.error().isPresent());

    }

    @Test
    public void testIllegalStateExceptionWhenDoneIsNotCalledWhileTracking() {
        messageTracker.begin();

        messageTracker.received(new Foo());

        try {
            messageTracker.received(new Foo());
            fail("Expected an IllegalStateException");
        } catch (IllegalStateException e){

        }
    }

    @Test
    public void testNoIllegalStateExceptionWhenDoneIsNotCalledWhileNotTracking() {
        messageTracker.received(new Foo());
        messageTracker.received(new Foo());
    }

    @Test
    public void testDelayInFirstExpectedMessageArrival(){
        messageTracker.begin();

        ticker.increment(MILLISECONDS.toNanos(20));

        MessageTracker.Context context = messageTracker.received(new Foo());

        Assert.assertEquals(true, context.error().isPresent());

        MessageTracker.Error error = context.error().get();

        Assert.assertEquals(null, error.getLastExpectedMessage());
        Assert.assertEquals(Foo.class, error.getCurrentExpectedMessage().getClass());

        String errorString = error.toString();
        Assert.assertTrue(errorString.contains("Last Expected Message = null"));

        LOG.error("An error occurred : {}", error);
    }

    @Test
    public void testCallingBeginDoesNotResetWatch() {
        messageTracker.begin();

        ticker.increment(MILLISECONDS.toNanos(20));

        messageTracker.begin();

        MessageTracker.Context context = messageTracker.received(new Foo());

        Assert.assertEquals(true, context.error().isPresent());

    }

    @Test
    public void testMessagesSinceLastExpectedMessage() {

        messageTracker.begin();

        try (MessageTracker.Context ctx = messageTracker.received(45)) {
            Assert.assertEquals(false, ctx.error().isPresent());
        }
        try (MessageTracker.Context ctx = messageTracker.received(45L)) {
            Assert.assertEquals(false, ctx.error().isPresent());
        }

        List<MessageTracker.MessageProcessingTime> processingTimeList =
                messageTracker.getMessagesSinceLastExpectedMessage();

        Assert.assertEquals(2, processingTimeList.size());

        assertEquals(Integer.class, processingTimeList.get(0).getMessageClass());
        assertEquals(Long.class, processingTimeList.get(1).getMessageClass());

    }

}