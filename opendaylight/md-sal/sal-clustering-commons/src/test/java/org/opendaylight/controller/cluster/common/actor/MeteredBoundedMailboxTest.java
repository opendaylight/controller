/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

public class MeteredBoundedMailboxTest {

    private static ActorSystem actorSystem;
    private static CommonConfig config;
    private final ReentrantLock lock = new ReentrantLock();

    @BeforeClass
    public static void setUp() throws Exception {
        config = new CommonConfig.Builder<>("testsystem").withConfigReader(new AkkaConfigurationReader() {
            @Override
            public Config read() {
                return ConfigFactory.load();
            }
        }).build();
        actorSystem = ActorSystem.create("testsystem", config.get());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (actorSystem != null) {
            actorSystem.terminate();
            actorSystem = null;
        }
    }

    @Test
    public void shouldSendMsgToDeadLetterWhenQueueIsFull() throws InterruptedException {
        final JavaTestKit mockReceiver = new JavaTestKit(actorSystem);
        actorSystem.eventStream().subscribe(mockReceiver.getRef(), DeadLetter.class);


        final FiniteDuration TWENTY_SEC = new FiniteDuration(20, TimeUnit.SECONDS);

        ActorRef pingPongActor = actorSystem.actorOf(PingPongActor.props(lock).withMailbox(config.getMailBoxName()),
                                                     "pingpongactor");

        actorSystem.mailboxes().settings();
        lock.lock();
        //queue capacity = 10
        //need to send 12 messages; 1 message is dequeued and actor waits on lock,
        //2nd to 11th messages are put on the queue
        //12th message is sent to dead letter.
        for (int i=0;i<12;i++){
            pingPongActor.tell("ping", mockReceiver.getRef());
        }

        mockReceiver.expectMsgClass(TWENTY_SEC, DeadLetter.class);

        lock.unlock();

        Object[] eleven = mockReceiver.receiveN(11, TWENTY_SEC);
    }

    /**
     * For testing
     */
    public static class PingPongActor extends UntypedActor{

        ReentrantLock lock;

        private PingPongActor(final ReentrantLock lock){
            this.lock = lock;
        }

        public static Props props(final ReentrantLock lock){
            return Props.create(PingPongActor.class, lock);
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            lock.lock();
            try {
                if ("ping".equals(message)) {
                    getSender().tell("pong", getSelf());
                }
            } finally {
                lock.unlock();
            }
        }
    }
}