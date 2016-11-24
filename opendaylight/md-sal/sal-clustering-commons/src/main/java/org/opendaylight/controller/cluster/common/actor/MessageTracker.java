/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageTracker is a diagnostic utility class to be used for figuring out why a certain message which was
 * expected to arrive in a given time interval does not arrive. It attempts to keep track of all the messages that
 * received between the arrival of two instances of the same message and the amount of time it took to process each
 * of those messages.
 * <br>
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
 *     try (MessageTracker.Context context = tracker.received(message)) {
 *
 *         if (context.error().isPresent()){
 *             LOG.error("{}", context.error().get());
 *         }
 *
 *         // Some custom processing
 *         process(message);
 *     }
 *
 * </pre>
 */
@Beta
@NotThreadSafe
public final class MessageTracker {
    public abstract static class Context implements AutoCloseable {
        Context() {
            // Hidden to prevent outside instantiation
        }

        public abstract Optional<Error> error();

        @Override
        public abstract void close();
    }

    public interface Error {
        Object getLastExpectedMessage();

        Object getCurrentExpectedMessage();

        List<MessageProcessingTime> getMessageProcessingTimesSinceLastExpectedMessage();
    }


    public static final class MessageProcessingTime {
        private final Class<?> messageClass;
        private final long elapsedTimeInNanos;

        MessageProcessingTime(final Class<?> messageClass, final long elapsedTimeInNanos) {
            this.messageClass = Preconditions.checkNotNull(messageClass);
            this.elapsedTimeInNanos = elapsedTimeInNanos;
        }

        @Override
        public String toString() {
            return "MessageProcessingTime [messageClass=" + messageClass + ", elapsedTimeInMillis="
                   + NANOSECONDS.toMillis(elapsedTimeInNanos) + "]";
        }


        public Class<?> getMessageClass() {
            return messageClass;
        }

        public long getElapsedTimeInNanos() {
            return elapsedTimeInNanos;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MessageTracker.class);
    private static final Context NO_OP_CONTEXT = new Context() {
        @Override
        public void close() {
            // No-op
        }

        @Override
        public Optional<Error> error() {
            return Optional.absent();
        }
    };

    private final List<MessageProcessingTime> messagesSinceLastExpectedMessage = new LinkedList<>();

    private final CurrentMessageContext currentMessageContext;

    private final Stopwatch expectedMessageWatch;

    private final Class<?> expectedMessageClass;

    private final long expectedArrivalInterval;

    private final Ticker ticker;

    private Object lastExpectedMessage;

    @VisibleForTesting
    MessageTracker(final Class<?> expectedMessageClass, final long expectedArrivalIntervalInMillis,
            final Ticker ticker) {
        Preconditions.checkArgument(expectedArrivalIntervalInMillis >= 0);
        this.expectedMessageClass = Preconditions.checkNotNull(expectedMessageClass);
        this.expectedArrivalInterval = MILLISECONDS.toNanos(expectedArrivalIntervalInMillis);
        this.ticker = Preconditions.checkNotNull(ticker);
        this.expectedMessageWatch = Stopwatch.createUnstarted(ticker);
        this.currentMessageContext = new CurrentMessageContext();
    }

    /**
     * Constructs an instance.
     *
     * @param expectedMessageClass the class of the message to track
     * @param expectedArrivalIntervalInMillis the expected arrival interval between two instances of the expected
     *                                        message
     */
    public MessageTracker(final Class<?> expectedMessageClass, final long expectedArrivalIntervalInMillis) {
        this(expectedMessageClass, expectedArrivalIntervalInMillis, Ticker.systemTicker());
    }

    public void begin() {
        if (!expectedMessageWatch.isRunning()) {
            LOG.trace("Started tracking class {} timeout {}ns", expectedMessageClass, expectedArrivalInterval);
            expectedMessageWatch.start();
        }
    }

    public Context received(final Object message) {
        if (!expectedMessageWatch.isRunning()) {
            return NO_OP_CONTEXT;
        }

        if (expectedMessageClass.isInstance(message)) {
            final long actualElapsedTime = expectedMessageWatch.elapsed(NANOSECONDS);
            if (actualElapsedTime > expectedArrivalInterval) {
                return new ErrorContext(message, new FailedExpectation(lastExpectedMessage, message,
                        messagesSinceLastExpectedMessage, expectedArrivalInterval, actualElapsedTime));
            }
            lastExpectedMessage = message;
            messagesSinceLastExpectedMessage.clear();
            expectedMessageWatch.reset().start();
        }

        currentMessageContext.reset(message);
        return currentMessageContext;
    }

    void processed(final Object message, final long messageElapseTimeInNanos) {
        if (expectedMessageWatch.isRunning() && !expectedMessageClass.isInstance(message)) {
            messagesSinceLastExpectedMessage.add(new MessageProcessingTime(message.getClass(),
                messageElapseTimeInNanos));
        }
    }

    public List<MessageProcessingTime> getMessagesSinceLastExpectedMessage() {
        return ImmutableList.copyOf(messagesSinceLastExpectedMessage);
    }

    private static final class FailedExpectation implements Error {
        private final Object lastExpectedMessage;
        private final Object currentExpectedMessage;
        private final List<MessageProcessingTime> messagesSinceLastExpectedMessage;
        private final long expectedTimeInMillis;
        private final long actualTimeInMillis;

        FailedExpectation(final Object lastExpectedMessage, final Object message,
                final List<MessageProcessingTime> messagesSinceLastExpectedMessage, final long expectedTimeNanos,
                final long actualTimeNanos) {
            this.lastExpectedMessage = lastExpectedMessage;
            this.currentExpectedMessage = message;
            this.messagesSinceLastExpectedMessage = ImmutableList.copyOf(messagesSinceLastExpectedMessage);
            this.expectedTimeInMillis = NANOSECONDS.toMillis(expectedTimeNanos);
            this.actualTimeInMillis = NANOSECONDS.toMillis(actualTimeNanos);
        }

        @Override
        public Object getLastExpectedMessage() {
            return lastExpectedMessage;
        }

        @Override
        public Object getCurrentExpectedMessage() {
            return currentExpectedMessage;
        }

        @Override
        public List<MessageProcessingTime>  getMessageProcessingTimesSinceLastExpectedMessage() {
            return messagesSinceLastExpectedMessage;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("\n> Last Expected Message = ").append(lastExpectedMessage);
            builder.append("\n> Current Expected Message = ").append(currentExpectedMessage);
            builder.append("\n> Expected time in between messages = ").append(expectedTimeInMillis);
            builder.append("\n> Actual time in between messages = ").append(actualTimeInMillis);
            for (MessageProcessingTime time : messagesSinceLastExpectedMessage) {
                builder.append("\n\t> ").append(time);
            }
            return builder.toString();
        }
    }

    private abstract class AbstractTimedContext extends Context {
        abstract Object message();

        abstract Stopwatch stopTimer();

        @Override
        public final void close() {
            processed(message(), stopTimer().elapsed(NANOSECONDS));
        }
    }

    private final class CurrentMessageContext extends AbstractTimedContext {
        private final Stopwatch stopwatch = Stopwatch.createUnstarted(ticker);
        private Object message;

        void reset(final Object newMessage) {
            this.message = Preconditions.checkNotNull(newMessage);
            Preconditions.checkState(!stopwatch.isRunning(),
                "Trying to reset a context that is not done (%s). currentMessage = %s", this, newMessage);
            stopwatch.start();
        }

        @Override
        Object message() {
            return message;
        }

        @Override
        Stopwatch stopTimer() {
            return stopwatch.stop();
        }

        @Override
        public Optional<Error> error() {
            return Optional.absent();
        }
    }

    private final class ErrorContext extends AbstractTimedContext {
        private final Stopwatch stopwatch = Stopwatch.createStarted(ticker);
        private final Object message;
        private final Error error;

        ErrorContext(final Object message, final Error error) {
            this.message = Preconditions.checkNotNull(message);
            this.error = Preconditions.checkNotNull(error);
        }

        @Override
        Object message() {
            return message;
        }

        @Override
        Stopwatch stopTimer() {
            return stopwatch.stop();
        }

        @Override
        public Optional<Error> error() {
            return Optional.of(error);
        }
    }
}
