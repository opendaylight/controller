package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageTrackerTest {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private class Foo {}

    @Test
    public void testNoTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.done();

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        context2.done();

    }

    @Test
    public void testFailedExpectationOnTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.done();

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        Assert.assertEquals(true, context2.error().isPresent());
        Assert.assertEquals(0, context2.error().get().getMessageProcessingTimesSinceLastExpectedMessage().size());

    }

    @Test
    public void testFailedExpectationOnTrackingWithMessagesInBetween(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.done();

        messageTracker.received("A").done();
        messageTracker.received(Long.valueOf(10)).done();
        MessageTracker.Context c = messageTracker.received(Integer.valueOf(100));

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        c.done();

        MessageTracker.Context context2 = messageTracker.received(new Foo());

        Assert.assertEquals(true, context2.error().isPresent());

        MessageTracker.Error error = context2.error().get();

        List<MessageTracker.MessageProcessingTime> messageProcessingTimes =
                error.getMessageProcessingTimesSinceLastExpectedMessage();

        Assert.assertEquals(3, messageProcessingTimes.size());

        Assert.assertEquals(String.class, messageProcessingTimes.get(0).getMessageClass());
        Assert.assertEquals(Long.class, messageProcessingTimes.get(1).getMessageClass());
        Assert.assertEquals(Integer.class, messageProcessingTimes.get(2).getMessageClass());
        Assert.assertTrue(messageProcessingTimes.get(2).getElapsedTimeInNanos() > TimeUnit.MILLISECONDS.toNanos(10));
        Assert.assertEquals(Foo.class, error.getLastExpectedMessage().getClass());
        Assert.assertEquals(Foo.class, error.getCurrentExpectedMessage().getClass());

        LOG.error("An error occurred : {}" , error);

    }


    @Test
    public void testMetExpectationOnTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(new Foo());
        context1.done();

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.received(new Foo());
        Assert.assertEquals(false, context2.error().isPresent());

    }

    @Test
    public void testIllegalStateExceptionWhenDoneIsNotCalledWhileTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        messageTracker.received(new Foo());

        try {
            messageTracker.received(new Foo());
            fail("Expected an IllegalStateException");
        } catch (IllegalStateException e){

        }
    }

    @Test
    public void testNoIllegalStateExceptionWhenDoneIsNotCalledWhileNotTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);

        messageTracker.received(new Foo());
        messageTracker.received(new Foo());
    }

    @Test
    public void testDelayInFirstExpectedMessageArrival(){

        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

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
    public void testCallingBeginDoesNotResetWatch(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        messageTracker.begin();

        MessageTracker.Context context = messageTracker.received(new Foo());

        Assert.assertEquals(true, context.error().isPresent());

    }

    @Test
    public void testMessagesSinceLastExpectedMessage(){

        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.begin();

        MessageTracker.Context context1 = messageTracker.received(Integer.valueOf(45)).done();

        Assert.assertEquals(false, context1.error().isPresent());

        MessageTracker.Context context2 = messageTracker.received(Long.valueOf(45)).done();

        Assert.assertEquals(false, context2.error().isPresent());

        List<MessageTracker.MessageProcessingTime> processingTimeList =
                messageTracker.getMessagesSinceLastExpectedMessage();

        Assert.assertEquals(2, processingTimeList.size());

        assertEquals(Integer.class, processingTimeList.get(0).getMessageClass());
        assertEquals(Long.class, processingTimeList.get(1).getMessageClass());

    }

}