/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MessageTracker is a diagnostic utility class to be used for figuring out why a certain message which was
 * expected to arrive in a given time interval does not arrive. It attempts to keep track of all the messages that
 * received between the arrival of two instances of the same message and the amount of time it took to process each
 * of those messages.
 * <br/>
 * Usage of the API is as follows,
 * <pre>
 *
 *      // Track the Foo class, Here we expect to see a message of type Foo come in every 10 millis
 *     MessageTracker tracker = new MessageTracker(Foo.class, 10);
 *
 *     // Begin the tracking process. If this is not called then calling received and done on the resultant Context
 *     // will do nothing
 *     tracker.begin();
 *
 *     .....
 *
 *     MessageTracker.Context context = tracker.received(message);
 *
 *     if(context.error().isPresent()){
 *         LOG.error("{}", context.error().get());
 *     }
 *
 *     // Some custom processing
 *     process(message);
 *
 *     context.done();
 *
 * </pre>
 */
public class MessageTracker {

    private static final Context NO_OP_CONTEXT = new NoOpContext();

    private final Class expectedMessageClass;

    private final long expectedArrivalInterval;

    private final List<MessageProcessingTime> messagesSinceLastExpectedMessage = new LinkedList<>();

    private Stopwatch expectedMessageWatch;

    private boolean enabled = false;

    private Object lastExpectedMessage;

    private Object currentMessage;

    private final CurrentMessageContext currentMessageContext = new CurrentMessageContext();

    /**
     *
     * @param expectedMessageClass The class of the message to track
     * @param expectedArrivalIntervalInMillis The expected arrival interval between two instances of the expected
     *                                        message
     */
    public MessageTracker(Class expectedMessageClass, long expectedArrivalIntervalInMillis){
        this.expectedMessageClass = expectedMessageClass;
        this.expectedArrivalInterval = expectedArrivalIntervalInMillis;
    }

    public void begin(){
        if(enabled) {
            return;
        }
        enabled = true;
        expectedMessageWatch = Stopwatch.createStarted();
    }

    public Context received(Object message){
        if(!enabled) {
            return NO_OP_CONTEXT;
        }
        this.currentMessage = message;
        if(expectedMessageClass.isInstance(message)){
            long actualElapsedTime = expectedMessageWatch.elapsed(TimeUnit.MILLISECONDS);
            if(actualElapsedTime > expectedArrivalInterval){
                return new ErrorContext(message, Optional.of(new FailedExpectation(lastExpectedMessage, message,
                        ImmutableList.copyOf(messagesSinceLastExpectedMessage), expectedArrivalInterval,
                        actualElapsedTime)));
            }
            this.lastExpectedMessage = message;
            this.messagesSinceLastExpectedMessage.clear();
        }

        currentMessageContext.reset();
        return currentMessageContext;
    }

    private void processed(Object message, long messageElapseTimeInNanos){
        if(!enabled) {
            return;
        }
        if(!expectedMessageClass.isInstance(message)){
            this.messagesSinceLastExpectedMessage.add(new MessageProcessingTime(message.getClass(), messageElapseTimeInNanos));
        }
    }

    public List<MessageProcessingTime> getMessagesSinceLastExpectedMessage(){
        return ImmutableList.copyOf(this.messagesSinceLastExpectedMessage);
    }

    public static class MessageProcessingTime {
        private final Class messageClass;
        private final long elapsedTimeInNanos;

        MessageProcessingTime(Class messageClass, long elapsedTimeInNanos){
            this.messageClass = messageClass;
            this.elapsedTimeInNanos = elapsedTimeInNanos;
        }

        @Override
        public String toString() {
            return "MessageProcessingTime{" +
                    "messageClass=" + messageClass.getSimpleName() +
                    ", elapsedTimeInMillis=" + TimeUnit.NANOSECONDS.toMillis(elapsedTimeInNanos) +
                    '}';
        }

        public Class getMessageClass() {
            return messageClass;
        }

        public long getElapsedTimeInNanos() {
            return elapsedTimeInNanos;
        }
    }

    public interface Error {
        Object getLastExpectedMessage();
        Object getCurrentExpectedMessage();
        List<MessageProcessingTime> getMessageProcessingTimesSinceLastExpectedMessage();
    }

    private class FailedExpectation implements Error {

        private final Object lastExpectedMessage;
        private final Object currentExpectedMessage;
        private final List<MessageProcessingTime> messagesSinceLastExpectedMessage;
        private final long expectedTimeInMillis;
        private final long actualTimeInMillis;

        public FailedExpectation(Object lastExpectedMessage, Object message, List<MessageProcessingTime> messagesSinceLastExpectedMessage, long expectedTimeInMillis, long actualTimeInMillis) {
            this.lastExpectedMessage = lastExpectedMessage;
            this.currentExpectedMessage = message;
            this.messagesSinceLastExpectedMessage = messagesSinceLastExpectedMessage;
            this.expectedTimeInMillis = expectedTimeInMillis;
            this.actualTimeInMillis = actualTimeInMillis;
        }

        public Object getLastExpectedMessage() {
            return lastExpectedMessage;
        }

        public Object getCurrentExpectedMessage() {
            return currentExpectedMessage;
        }

        public List<MessageProcessingTime>  getMessageProcessingTimesSinceLastExpectedMessage() {
            return messagesSinceLastExpectedMessage;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("\n> Last Expected Message = " + lastExpectedMessage);
            builder.append("\n> Current Expected Message = " + currentExpectedMessage);
            builder.append("\n> Expected time in between messages = " + expectedTimeInMillis);
            builder.append("\n> Actual time in between messages = " + actualTimeInMillis);
            for (MessageProcessingTime time : messagesSinceLastExpectedMessage) {
                builder.append("\n\t> ").append(time.toString());
            }
            return builder.toString();
        }

    }

    public interface Context {
        Context done();
        Optional<? extends Error> error();
    }

    private static class NoOpContext implements Context {

        @Override
        public Context done() {
            return this;
        }

        @Override
        public Optional<Error> error() {
            return Optional.absent();
        }
    }

    private class CurrentMessageContext implements Context {
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean done = true;

        public void reset(){
            Preconditions.checkState(done,
                    String.format("Trying to reset a context that is not done (%s). currentMessage = %s", done, currentMessage));
            done = false;
            stopwatch.reset().start();
        }

        @Override
        public Context done() {
            processed(currentMessage, stopwatch.elapsed(TimeUnit.NANOSECONDS));
            done = true;
            return this;
        }

        @Override
        public Optional<? extends Error> error() {
            return Optional.absent();
        }
    }

    private class ErrorContext implements Context {
        Object message;
        private final Optional<? extends Error> error;
        Stopwatch stopwatch;

        ErrorContext(Object message, Optional<? extends Error> error){
            this.message = message;
            this.error = error;
            this.stopwatch = Stopwatch.createStarted();
        }

        @Override
        public Context done(){
            processed(message, this.stopwatch.elapsed(TimeUnit.NANOSECONDS));
            this.stopwatch.stop();
            return this;
        }

        @Override
        public Optional<? extends Error> error() {
            return error;
        }
    }
}
