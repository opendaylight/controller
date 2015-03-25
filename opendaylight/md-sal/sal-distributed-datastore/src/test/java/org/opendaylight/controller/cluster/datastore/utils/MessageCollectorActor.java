/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * MessageCollectorActor collects messages as it receives them. It can send
 * those collected messages to any sender which sends it the "messages" message
 * <p>
 *     This class would be useful as a mock to test whether messages were sent
 *     to a remote actor or not.
 * </p>
 */
public class MessageCollectorActor extends UntypedActor {
    private final List<Object> messages = new ArrayList<>();

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof String){
            if("messages".equals(message)){
                getSender().tell(new ArrayList(messages), getSelf());
            }
        } else {
            messages.add(message);
        }
    }

    public void clear() {
        messages.clear();
    }

    public static List<Object> getAllMessages(ActorRef actor) throws Exception {
        FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
        Timeout operationTimeout = new Timeout(operationDuration);
        Future<Object> future = Patterns.ask(actor, "messages", operationTimeout);

        try {
            return (List<Object>) Await.result(future, operationDuration);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Get the first message that matches the specified class
     * @param actor
     * @param clazz
     * @return
     */
    public static Object getFirstMatching(ActorRef actor, Class<?> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                return message;
            }
        }

        return null;
    }

    public static List<Object> getAllMatching(ActorRef actor, Class<?> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        List<Object> output = Lists.newArrayList();

        for(Object message : allMessages){
            if(message.getClass().equals(clazz)){
                output.add(message);
            }
        }

        return output;
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz) {
        int count = 5000 / 50;
        for(int i = 0; i < count; i++) {
            try {
                T message = (T) getFirstMatching(actor, clazz);
                if(message != null) {
                    return message;
                }
            } catch (Exception e) {}

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Did not receive message of type " + clazz);
        return null;
    }
}
