/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.utils;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;


public class MessageCollectorActor extends UntypedActor {
    private static final String CLEAR_MESSAGES = "clear-messages";

    private final List<Object> messages = new ArrayList<>();

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof String){
            if("get-all-messages".equals(message)){
                getSender().tell(new ArrayList(messages), getSelf());
            } else if(CLEAR_MESSAGES.equals(message)) {
                messages.clear();
            }
        } else {
            messages.add(message);
        }
    }

    public void clear() {
        messages.clear();
    }

    public static void clearMessages(ActorRef actor) {
        actor.tell(CLEAR_MESSAGES, ActorRef.noSender());
    }

    public static List<Object> getAllMessages(ActorRef actor) throws Exception {
        FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
        Timeout operationTimeout = new Timeout(operationDuration);
        Future<Object> future = Patterns.ask(actor, "get-all-messages", operationTimeout);

        try {
            return (List<Object>) Await.result(future, operationDuration);
        } catch (Exception e) {
            throw e;
        }
    }

    public static <T> List<T> expectMatching(ActorRef actor, Class<T> clazz, int count) {
        int timeout = 5000;
        List<T> messages = Collections.emptyList();
        for(int i = 0; i < timeout / 50; i++) {
            try {
                messages = (List<T>) getAllMatching(actor, clazz);
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

    /**
     * Get the first message that matches the specified class
     * @param actor
     * @param clazz
     * @return
     */
    public static <T> T getFirstMatching(ActorRef actor, Class<T> clazz) throws Exception {
        for(int i = 0; i < 50; i++) {
            List<Object> allMessages = getAllMessages(actor);

            for(Object message : allMessages){
                if(message.getClass().equals(clazz)){
                    return (T) message;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        return null;
    }

    public static <T> List<T> getAllMatching(ActorRef actor, Class<?> clazz) throws Exception {
        List<T> allMessages = (List<T>) getAllMessages(actor);

        List<T> output = Lists.newArrayList();

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                output.add((T) message);
            }
        }

        return output;
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz) {
        return expectFirstMatching(actor, clazz, 5000);
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz, long timeout) {
        int count = (int) (timeout / 50);
        for(int i = 0; i < count; i++) {
            try {
                T message = getFirstMatchingNoWait(actor, clazz);
                if(message != null) {
                    return message;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Did not receive message of type " + clazz);
        return null;
    }

    public static <T> T getFirstMatchingNoWait(ActorRef actor, Class<T> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                return (T) message;
            }
        }

        return null;
    }
}
