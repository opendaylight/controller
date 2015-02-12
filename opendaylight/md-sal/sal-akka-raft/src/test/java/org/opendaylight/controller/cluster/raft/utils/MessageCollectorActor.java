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
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;


public class MessageCollectorActor extends UntypedActor {
    private final List<Object> messages = new ArrayList<>();

    @Override public void onReceive(Object message) throws Exception {
        if(message instanceof String){
            if("get-all-messages".equals(message)){
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
        Future<Object> future = Patterns.ask(actor, "get-all-messages", operationTimeout);

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

}
