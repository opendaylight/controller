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

    private final List<Object> messages = new ArrayList<>();

    @Override public void onReceive(Object message) throws Exception {
        if(message.equals(ARE_YOU_READY)) {
            getSender().tell("yes", getSelf());
            return;
        }

        if(message instanceof String){
            if("get-all-messages".equals(message)){
                getSender().tell(new ArrayList<>(messages), getSelf());
            }
        } else if(message != null) {
            messages.add(message);
        }
    }

    public void clear() {
        messages.clear();
    }

    public static List<Object> getAllMessages(ActorRef actor) throws Exception {
        FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
        Timeout operationTimeout = new Timeout(operationDuration);
        Future<Object> future = Patterns.ask(actor, "get-all-messages", operationTimeout);

        return (List<Object>) Await.result(future, operationDuration);
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
                return (T) message;
            }
        }

        return null;
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz) throws Exception {
        for(int i = 0; i < 50; i++) {
            T message = getFirstMatching(actor, clazz);
            if(message != null) {
                return message;
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Did not receive message of type " + clazz);
        return null;
    }

    public static <T> List<T> getAllMatching(ActorRef actor, Class<T> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        List<T> output = Lists.newArrayList();

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                output.add((T) message);
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
}
