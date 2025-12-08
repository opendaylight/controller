/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2025 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.dispatch.ControlMessage;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Assert;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Interceptor around an actor using {@link Actor}.
 */
public final class MessageCollector implements AutoCloseable {
    /**
     * The actor providing the actual service.
     */
    public static class Actor extends UntypedAbstractActor {
        private static final String ARE_YOU_READY = "ARE_YOU_READY";

        public static final String GET_ALL_MESSAGES = "messages";

        private static final Object CLEAR_MESSAGES = new ControlMessage() {
            @Override
            public String toString() {
                return "clear-messages";
            }
        };

        private final List<Object> messages = new ArrayList<>();

        public Actor() {
            // Visible for Pekko
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            if (ARE_YOU_READY.equals(message)) {
                getSender().tell("yes", self());
            } else if (GET_ALL_MESSAGES.equals(message)) {
                getSender().tell(new ArrayList<>(messages), self());
            } else if (CLEAR_MESSAGES.equals(message)) {
                messages.clear();
            } else if (message != null) {
                messages.add(message);
            }
        }

        @SuppressWarnings({"unchecked", "checkstyle:illegalCatch"})
        public static List<Object> getAllMessages(final ActorRef actor) {
            FiniteDuration operationDuration = FiniteDuration.create(5, TimeUnit.SECONDS);
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
            final var allMessages = getAllMessages(actor);

            for (var message : allMessages) {
                if (matches(clazz, message)) {
                    return clazz.cast(message);
                }
            }

            return null;
        }

        @NonNullByDefault
        @SuppressWarnings("checkstyle:IllegalCatch")
        public static <T> List<T> expectMatching(final ActorRef actor, final Class<T> clazz, final int count) {
            return expectMatching(actor, clazz, count, msg -> true);
        }

        @NonNullByDefault
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

            throw new AssertionError(actor + ": Did not receive message of type " + clazz + ", Actual received was "
                + getAllMessages(actor), lastEx);
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
                        Assert.fail("Unexpected message received " + message);
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

            for (var message : allMessages) {
                if (matches(clazz, message)) {
                    output.add(clazz.cast(message));
                }
            }

            return output;
        }

        private static boolean matches(final Class<?> clazz, final Object obj) {
            return clazz.equals(obj.getClass()) || Modifier.isAbstract(clazz.getModifiers()) && clazz.isInstance(obj);
        }

        public static void waitUntilReady(final ActorRef actor) throws TimeoutException, InterruptedException {
            long timeout = 500;
            FiniteDuration duration = FiniteDuration.create(timeout, TimeUnit.MILLISECONDS);
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
            return Props.create(Actor.class);
        }
    }



    private final ActorRef actor;

    private MessageCollector(final ActorRef actor) {
        this.actor = requireNonNull(actor);
    }

    @NonNullByDefault
    public static MessageCollector of(final ActorSystem system, final String actorName) {
        return new MessageCollector(system.actorOf(Actor.props(), actorName));
    }

    @NonNullByDefault
    public static MessageCollector of(final TestActorFactory factory, final String prefix) {
        return new MessageCollector(factory.createActor(Actor.props(), factory.generateActorId(prefix)));
    }

    public ActorRef actor() {
        return actor;
    }

    public <T> List<T> expectMatching(final Class<T> clazz, final int count) {
        return Actor.expectMatching(actor, clazz, count);
    }

    public <T> T expectFirstMatching(final Class<T> clazz) {
        return Actor.expectFirstMatching(actor, clazz);
    }

    public void assertNoneMatching(final Class<?> clazz) {
        Actor.assertNoneMatching(actor, clazz);
    }

    public void clearMessages() {
        Actor.clearMessages(actor);
    }

    public <T> List<T> getAllMatching(final Class<T> clazz) {
        return Actor.expectMatching(actor, clazz);
    }

    @Override
    public void close() {
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }
}
