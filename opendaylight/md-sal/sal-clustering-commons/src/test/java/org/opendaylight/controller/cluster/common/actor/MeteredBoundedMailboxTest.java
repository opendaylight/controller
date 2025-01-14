/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.DeadLetter;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.testkit.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

public class MeteredBoundedMailboxTest {

    private static ActorSystem actorSystem;
    private static CommonConfig config;
    private final ReentrantLock lock = new ReentrantLock();

    @BeforeClass
    public static void setUp() {
        config = new CommonConfig.Builder<>("testsystem").withConfigReader(ConfigFactory::load).build();
        actorSystem = ActorSystem.create("testsystem", config.get());
    }

    @AfterClass
    public static void tearDown() {
        if (actorSystem != null) {
            actorSystem.terminate();
            actorSystem = null;
        }
    }

    @Test
    public void shouldSendMsgToDeadLetterWhenQueueIsFull() {
        final TestKit mockReceiver = new TestKit(actorSystem);
        actorSystem.eventStream().subscribe(mockReceiver.testActor(), DeadLetter.class);

        final FiniteDuration twentySeconds = new FiniteDuration(20, TimeUnit.SECONDS);

        ActorRef pingPongActor = actorSystem.actorOf(PingPongActor.props(lock).withMailbox(config.getMailBoxName()),
                                                     "pingpongactor");

        actorSystem.mailboxes().settings();
        lock.lock();
        try {
            //queue capacity = 10
            //need to send 12 messages; 1 message is dequeued and actor waits on lock,
            //2nd to 11th messages are put on the queue
            //12th message is sent to dead letter.
            for (int i = 0; i < 12; i++) {
                pingPongActor.tell("ping", mockReceiver.testActor());
            }

            mockReceiver.expectMsgClass(twentySeconds, DeadLetter.class);
        } finally {
            lock.unlock();
        }

        mockReceiver.receiveN(11, twentySeconds);
    }

    /**
     * For testing.
     */
    public static class PingPongActor extends UntypedAbstractActor {
        ReentrantLock lock;

        PingPongActor(final ReentrantLock lock) {
            this.lock = lock;
        }

        public static Props props(final ReentrantLock lock) {
            return Props.create(PingPongActor.class, lock);
        }

        @Override
        public void onReceive(final Object message) {
            lock.lock();
            try {
                if ("ping".equals(message)) {
                    getSender().tell("pong", self());
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
