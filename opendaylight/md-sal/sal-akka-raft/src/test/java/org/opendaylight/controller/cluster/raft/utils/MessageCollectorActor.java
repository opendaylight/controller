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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    @Override public void onReceive(Object message) throws Exception {
        if(message.equals(ARE_YOU_READY)) {
            getSender().tell("yes", getSelf());
            return;
        }

        if(GET_ALL_MESSAGES.equals(message)) {
            getSender().tell(new ArrayList<>(messages), getSelf());
        } else if(CLEAR_MESSAGES.equals(message)) {
            clear();
        } else if(message != null) {
            messages.add(message);
        }
    }

    public void clear() {
        messages.clear();
    }

    private static List<Object> getAllMessages(ActorRef actor) throws Exception {
        FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
        Timeout operationTimeout = new Timeout(operationDuration);
        Future<Object> future = Patterns.ask(actor, GET_ALL_MESSAGES, operationTimeout);

        return (List<Object>) Await.result(future, operationDuration);
    }

    public static void clearMessages(ActorRef actor) {
        actor.tell(CLEAR_MESSAGES, ActorRef.noSender());
    }

    /**
     * Get the first message that matches the specified class
     * @param actor
     * @param clazz
     * @return
     */
    public static <T> T getFirstMatching(ActorRef actor, Class<T> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                return clazz.cast(message);
            }
        }

        return null;
    }

    public static <T> List<T> expectMatching(ActorRef actor, Class<T> clazz, int count) {
        return expectMatching(actor, clazz, count, msg -> true);
    }

    public static <T> List<T> expectMatching(ActorRef actor, Class<T> clazz, int count,
            Predicate<T> matcher) {
        int timeout = 5000;
        List<T> messages = Collections.emptyList();
        for(int i = 0; i < timeout / 50; i++) {
            try {
                messages = getAllMatching(actor, clazz);
                Iterator<T> iter = messages.iterator();
                while(iter.hasNext()) {
                    if(!matcher.apply(iter.next())) {
                        iter.remove();
                    }
                }

                if(messages.size() >= count) {
                    return messages;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail(String.format("Expected %d messages of type %s. Actual received was %d: %s", count, clazz,
                messages.size(), messages));
        return null;
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz) {
        return expectFirstMatching(actor, clazz, 5000);
    }


    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz, long timeout) {
        int count = (int) (timeout / 50);
        for(int i = 0; i < count; i++) {
            try {
                T message = getFirstMatching(actor, clazz);
                if(message != null) {
                    return message;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Did not receive message of type " + clazz);
        return null;
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz, Predicate<T> matcher) {
        int timeout = 5000;
        T lastMessage = null;
        for(int i = 0; i < timeout / 50; i++) {
            try {
                List<T> messages = getAllMatching(actor, clazz);
                for(T msg: messages) {
                    if(matcher.apply(msg)) {
                        return msg;
                    }

                    lastMessage = msg;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail(String.format("Expected specific message of type %s. Last message received was: %s", clazz, lastMessage));
        return null;
    }

    public static <T> void assertNoneMatching(ActorRef actor, Class<T> clazz) {
        assertNoneMatching(actor, clazz, 5000);
    }

    public static <T> void assertNoneMatching(ActorRef actor, Class<T> clazz, long timeout) {
        int count = (int) (timeout / 50);
        for(int i = 0; i < count; i++) {
            try {
                T message = getFirstMatching(actor, clazz);
                if(message != null) {
                    Assert.fail("Unexpected message received" +  message.toString());
                    return;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        return;
    }


    public static <T> List<T> getAllMatching(ActorRef actor, Class<T> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        List<T> output = Lists.newArrayList();

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                output.add(clazz.cast(message));
            }
        }

        return output;
    }

    public static void waitUntilReady(ActorRef actor) throws Exception {
        long timeout = 500;
        FiniteDuration duration = Duration.create(timeout, TimeUnit.MILLISECONDS);
        for(int i = 0; i < 10; i++) {
            try {
                Await.ready(Patterns.ask(actor, ARE_YOU_READY, timeout), duration);
                return;
            } catch (TimeoutException e) {
            }
        }

        throw new TimeoutException("Actor not ready in time.");
    }

    public static Props props() {
        return Props.create(MessageCollectorActor.class);
    }
}
