/*
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others.  All rights reserved.
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
import org.junit.Assert;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public abstract class AbstractMessageCollector extends UntypedActor {
    protected static final String GET_ALL_MESSAGES = "messages";
    protected final List<Object> messages = new ArrayList<>();

    public void clear() {
        messages.clear();
    }

    private static List<Object> getAllMessages(ActorRef actor) throws Exception {
        FiniteDuration operationDuration = Duration.create(5, TimeUnit.SECONDS);
        Timeout operationTimeout = new Timeout(operationDuration);
        Future<Object> future = Patterns.ask(actor, GET_ALL_MESSAGES, operationTimeout);

        try {
            return (List<Object>) Await.result(future, operationDuration);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Get the first message that matches the specified class
     *
     * @param actor
     * @param clazz
     * @return
     */
    public static <T> T getFirstMatching(ActorRef actor, Class<T> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        for (Object message : allMessages) {
            if (message.getClass().equals(clazz)) {
                return clazz.cast(message);
            }
        }

        return null;
    }

    public static <T> List<T> getAllMatching(ActorRef actor, Class<T> clazz) throws Exception {
        List<Object> allMessages = getAllMessages(actor);

        List<T> output = Lists.newArrayList();

        for (Object message : allMessages) {
            if (message.getClass().equals(clazz)) {
                output.add(clazz.cast(message));
            }
        }

        return output;
    }

    public static <T> T expectFirstMatching(ActorRef actor, Class<T> clazz, long timeout) {
        int count = (int) (timeout / 50);
        for (int i = 0; i < count; i++) {
            try {
                T message = getFirstMatching(actor, clazz);
                if (message != null) {
                    return message;
                }
            } catch (Exception e) {
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Did not receive message of type " + clazz);
        return null;
    }

}
