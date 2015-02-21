package org.opendaylight.controller.cluster.datastore.utils;

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

        MessageTracker.Context context1 = messageTracker.arrived(new Foo());
        context1.done();

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.arrived(new Foo());
        context2.done();

    }

    @Test
    public void testFailedExpectationOnTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.beginTracking();

        MessageTracker.Context context1 = messageTracker.arrived(new Foo());
        context1.done();

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.arrived(new Foo());
        Assert.assertEquals(true, context2.error().isPresent());
        Assert.assertEquals(0, context2.error().get().getMessageProcessingTimesSinceLastExpectedMessage().size());

    }

    @Test
    public void testFailedExpectationOnTrackingWithMessagesInBetween(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.beginTracking();

        MessageTracker.Context context1 = messageTracker.arrived(new Foo());
        context1.done();

        messageTracker.arrived("A").done();
        messageTracker.arrived(Long.valueOf(10)).done();
        MessageTracker.Context c = messageTracker.arrived(Integer.valueOf(100));

        Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);

        c.done();

        MessageTracker.Context context2 = messageTracker.arrived(new Foo());

        Assert.assertEquals(true, context2.error().isPresent());
        List<MessageTracker.MessageProcessingTime> messageProcessingTimes =
                context2.error().get().getMessageProcessingTimesSinceLastExpectedMessage();

        Assert.assertEquals(3, messageProcessingTimes.size());

        Assert.assertEquals(String.class, messageProcessingTimes.get(0).getMessageClass());
        Assert.assertEquals(Long.class, messageProcessingTimes.get(1).getMessageClass());
        Assert.assertEquals(Integer.class, messageProcessingTimes.get(2).getMessageClass());
        Assert.assertTrue(messageProcessingTimes.get(2).getElapsedTimeInNanos() > TimeUnit.MILLISECONDS.toNanos(10));

        LOG.error("An error occurred : {}" , context2.error().get());

    }


    @Test
    public void testMetExpectationOnTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.beginTracking();

        MessageTracker.Context context1 = messageTracker.arrived(new Foo());
        context1.done();

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);

        MessageTracker.Context context2 = messageTracker.arrived(new Foo());
        Assert.assertEquals(false, context2.error().isPresent());

    }

    @Test
    public void testIllegalStateExceptionWhenDoneIsNotCalledWhileTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);
        messageTracker.beginTracking();

        messageTracker.arrived(new Foo());

        try {
            messageTracker.arrived(new Foo());
            fail("Expected an IllegalStateException");
        } catch (IllegalStateException e){

        }
    }

    @Test
    public void testNoIllegalStateExceptionWhenDoneIsNotCalledWhileNotTracking(){
        MessageTracker messageTracker = new MessageTracker(Foo.class, 10);

        messageTracker.arrived(new Foo());
        messageTracker.arrived(new Foo());
    }

}