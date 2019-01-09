/*
 * Copyright (c) 2019 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Creator;
import akka.testkit.javadsl.TestKit;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class AbstractActorWithPersistenceFailureTrackingTest {
    private static final String PING_MSG = "ping";
    private static final String PONG_MSG = "pong";
    private static final String STOP_MSG = "stop";
    private static final FiniteDuration MIN_BACKOFF = Duration.create(100, TimeUnit.MILLISECONDS);
    private static final FiniteDuration MAX_BACKOFF = Duration.create(500, TimeUnit.MILLISECONDS);
    private static final FiniteDuration RESET_BACKOFF = Duration.create(300, TimeUnit.MILLISECONDS);

    private ActorSystem system;
    private CommonConfig config;

    @Before
    public void setUp() throws IOException {
        config = new CommonConfig.Builder<>("testsystem").withConfigReader(ConfigFactory::load).build();
        system = ActorSystem.create("testsystem", config.get());
    }

    @After
    public void tearDown() throws IOException {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    private static final class MockPersistentActor extends AbstractActorWithPersistenceFailureTracking {

        private static final String PERSISTENCE_ID = "mock-persistent-actor";

        private MockPersistentActor(final boolean backoffSupervised, final boolean persistFailed) {
            this.backoffSupervised.set(backoffSupervised);
            setPersistFailed(persistFailed);
        }

        private static Props props(final boolean backoffSupervised, final boolean persistFailed) {
            return Props.create(new MockPersistentActorCreator(backoffSupervised, persistFailed));
        }

        @Override
        public String persistenceId() {
            return PERSISTENCE_ID;
        }

        @Override
        public void onReceiveCommand(final Object message) throws Throwable {
            if (message instanceof String) {
                String msgString = (String) message;
                if (msgString.equals(PING_MSG)) {
                    getSender().tell(PONG_MSG, getSelf());
                } else if (msgString.equals(STOP_MSG)) {
                    getContext().stop(getSelf());
                }
            }
        }

        @Override
        public void onReceiveRecover(final Object message) throws Throwable {}

        @SuppressWarnings("serial")
        private static class MockPersistentActorCreator implements Creator<MockPersistentActor> {
            final boolean backoffSupervised;
            final boolean persistFailed;

            MockPersistentActorCreator(final boolean backoffSupervised, final boolean persistFailed) {
                this.backoffSupervised = backoffSupervised;
                this.persistFailed = persistFailed;
            }

            @Override
            public MockPersistentActor create() throws Exception {
                return new MockPersistentActor(backoffSupervised, persistFailed);
            }
        }
    }

    @Test
    public void testNonBackoffSupervisedActorNonPersistFailStop() {
        testStopBehavior(false, false);
    }

    @Test
    public void testNonBackoffSupervisedActorPersistFailStop() {
        testStopBehavior(false, true);
    }

    @Test
    public void testBackoffSupervisedActorNonPersistFailStop() {
        testStopBehavior(true, false);
    }

    @Test
    public void testBackoffSupervisedActorPersistFailStop() {
        testStopBehavior(true, true);
    }

    private void testStopBehavior(boolean backoffSupervised, boolean persistFailed) {
        TestKit kit = new TestKit(system);
        FiniteDuration waitDuration = kit.duration("200 millis");

        ActorRef testActor = createTestActor(backoffSupervised, persistFailed);

        testActor.tell(PING_MSG, kit.getRef());
        kit.expectMsg(waitDuration, PONG_MSG);

        testActor.tell(STOP_MSG, kit.getRef());

        testActor.tell(PING_MSG, kit.getRef());
        kit.expectNoMessage(waitDuration);

        Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);

        testActor.tell(PING_MSG, kit.getRef());
        if (backoffSupervised && persistFailed) {
            kit.expectMsg(waitDuration, PONG_MSG);
        } else {
            kit.expectNoMessage(waitDuration);
        }
    }

    private ActorRef createTestActor(boolean backoffSupervised, boolean persistFailed) {
        if (backoffSupervised) {
            return BackoffSupervisorUtils.createBackoffSupervisor(system, MIN_BACKOFF, MAX_BACKOFF, RESET_BACKOFF,
                    "mock-persistent-actor", null, null, MockPersistentActor.props(true, persistFailed));
        } else {
            return system.actorOf(MockPersistentActor.props(false, persistFailed));
        }
    }
}
