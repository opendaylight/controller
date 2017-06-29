/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class MessageCollectorActor extends UntypedActor {
    private static final String ARE_YOU_READY = "ARE_YOU_READY";
    public static final String GET_ALL_MESSAGES = "messages";
    private static final String CLEAR_MESSAGES = "clear-messages";

    private final List<Object> messages = new ArrayList<>();

    @Override public void onReceive(final Object message) throws Exception {
        if (ARE_YOU_READY.equals(message)) {
            getSender().tell("yes", getSelf());
        } else if (GET_ALL_MESSAGES.equals(message)) {
            getSender().tell(new ArrayList<>(messages), getSelf());
        } else if (CLEAR_MESSAGES.equals(message)) {
            clear();
        } else if (message != null) {
            messages.add(message);
        }
    }

    public void clear() {
        messages.clear();
    }

    @SuppressWarnings({"unchecked", "checkstyle:illegalCatch"})
    private static List<Object> getAllMessages(final ActorRef actor) {
        FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
        Timeout operationTimeout = new Timeout(operationDuration);
        Future<Object> future = Patterns.ask(actor, GET_ALL_MESSAGES, operationTimeout);

        try {
            return (List<Object>) Await.result(future, operationDuration);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearMessages(final ActorRef actor) {
        actor.tell(CLEAR_MESSAGES, ActorRef.noSender());
    }

    /**
     * Get the first message that matches the specified class.
     *
     * @param actor the MessageCollectorActor reference
     * @param clazz the class to match
     * @return the first matching message
     */
    public static <T> T getFirstMatching(final ActorRef actor, final Class<T> clazz) {
        List<Object> allMessages = getAllMessages(actor);

        for (Object message : allMessages) {
            if (message.getClass().equals(clazz)) {
                return clazz.cast(message);
            }
        }

        return null;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> List<T> expectMatching(final ActorRef actor, final Class<T> clazz, final int count) {
        return expectMatching(actor, clazz, count, msg -> true);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> List<T> expectMatching(final ActorRef actor, final Class<T> clazz, final int count,
            final Predicate<T> matcher) {
        int timeout = 5000;
        Exception lastEx = null;
        List<T> messages = Collections.emptyList();
        for (int i = 0; i < timeout / 50; i++) {
            try {
                messages = getAllMatching(actor, clazz);
                Iterables.removeIf(messages, Predicates.not(matcher));
                if (messages.size() >= count) {
                    return messages;
                }

                lastEx = null;
            } catch (Exception e)  {
                lastEx = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw new AssertionError(String.format("Expected %d messages of type %s. Actual received was %d: %s", count,
                clazz, messages.size(), messages), lastEx);
    }

    public static <T> T expectFirstMatching(final ActorRef actor, final Class<T> clazz) {
        return expectFirstMatching(actor, clazz, 5000);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> T expectFirstMatching(final ActorRef actor, final Class<T> clazz, final long timeout) {
        Exception lastEx = null;
        int count = (int) (timeout / 50);
        for (int i = 0; i < count; i++) {
            try {
                T message = getFirstMatching(actor, clazz);
                if (message != null) {
                    return message;
                }

                lastEx = null;
            } catch (Exception e) {
                lastEx = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw new AssertionError("Did not receive message of type " + clazz, lastEx);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> T expectFirstMatching(final ActorRef actor, final Class<T> clazz, final Predicate<T> matcher) {
        int timeout = 5000;
        Exception lastEx = null;
        T lastMessage = null;
        for (int i = 0; i < timeout / 50; i++) {
            try {
                List<T> messages = getAllMatching(actor, clazz);
                for (T msg : messages) {
                    if (matcher.apply(msg)) {
                        return msg;
                    }

                    lastMessage = msg;
                }

                lastEx = null;
            } catch (Exception e) {
                lastEx = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw new AssertionError(String.format("Expected specific message of type %s. Last message received was: %s",
                clazz, lastMessage), lastEx);
    }

    public static <T> void assertNoneMatching(final ActorRef actor, final Class<T> clazz) {
        assertNoneMatching(actor, clazz, 5000);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> void assertNoneMatching(final ActorRef actor, final Class<T> clazz, final long timeout) {
        Exception lastEx = null;
        int count = (int) (timeout / 50);
        for (int i = 0; i < count; i++) {
            try {
                T message = getFirstMatching(actor, clazz);
                if (message != null) {
                    Assert.fail("Unexpected message received" +  message.toString());
                    return;
                }

                lastEx = null;
            } catch (Exception e) {
                lastEx = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        if (lastEx != null) {
            Throwables.throwIfUnchecked(lastEx);
            throw new RuntimeException(lastEx);
        }

        return;
    }


    public static <T> List<T> getAllMatching(final ActorRef actor, final Class<T> clazz) {
        List<Object> allMessages = getAllMessages(actor);

        List<T> output = new ArrayList<>();

        for (Object message : allMessages) {
            if (message.getClass().equals(clazz)) {
                output.add(clazz.cast(message));
            }
        }

        return output;
    }

    public static void waitUntilReady(final ActorRef actor) throws TimeoutException, InterruptedException {
        long timeout = 500;
        FiniteDuration duration = Duration.create(timeout, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 10; i++) {
            try {
                Await.ready(Patterns.ask(actor, ARE_YOU_READY, timeout), duration);
                return;
            } catch (TimeoutException e) {
                // will fall through below
            }
        }

        throw new TimeoutException("Actor not ready in time.");
    }

    public static Props props() {
        return Props.create(MessageCollectorActor.class);
    }
}
