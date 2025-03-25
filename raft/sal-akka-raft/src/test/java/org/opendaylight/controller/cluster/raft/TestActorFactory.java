/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.Actor;
import org.apache.pekko.actor.ActorIdentity;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Identify;
import org.apache.pekko.actor.InvalidActorNameException;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * TestActorFactory provides methods to create both normal and test actors and to kill them when the factory is closed
 * The ideal usage for TestActorFactory is with try with resources.
 * <p/>
 * For example <br/>
 * <pre>
 *     try (TestActorFactory factory = new TestActorFactory(getSystem())){
 *         factory.createActor(props);
 *         factory.createTestActor(props);
 *         factory.generateActorId("leader-");
 *     }
 * </pre>
 */
public class TestActorFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TestActorFactory.class);

    private final ActorSystem system;
    private final List<ActorRef> createdActors = new ArrayList<>();
    private static int actorCount = 1;

    public TestActorFactory(final ActorSystem system) {
        this.system = system;
    }

    /**
     * Create a normal actor with an auto-generated name.
     *
     * @param props the actor Props
     * @return the ActorRef
     */
    public ActorRef createActor(final Props props) {
        ActorRef actorRef = system.actorOf(props);
        return addActor(actorRef, true);
    }

    /**
     * Create a normal actor with the passed in name.
     *
     * @param props the actor Props
     * @param actorId name of actor
     * @return the ActorRef
     */
    public ActorRef createActor(final Props props, final String actorId) {
        ActorRef actorRef = system.actorOf(props, actorId);
        return addActor(actorRef, true);
    }

    /**
     * Create a normal actor with the passed in name w/o verifying that the actor is ready.
     *
     * @param props the actor Props
     * @param actorId name of actor
     * @return the ActorRef
     */
    public ActorRef createActorNoVerify(final Props props, final String actorId) {
        ActorRef actorRef = system.actorOf(props, actorId);
        return addActor(actorRef, false);
    }

    /**
     * Create a test actor with the passed in name.
     *
     * @param props the actor Props
     * @param actorId name of actor
     * @param <T> the actor type
     * @return the ActorRef
     */
    @SuppressWarnings("unchecked")
    public <T extends Actor> TestActorRef<T> createTestActor(final Props props, final String actorId) {
        InvalidActorNameException lastError = null;
        for (int i = 0; i < 10; i++) {
            try {
                TestActorRef<T> actorRef = TestActorRef.create(system, props, actorId);
                return (TestActorRef<T>) addActor(actorRef, true);
            } catch (InvalidActorNameException e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    /**
     * Create a test actor with an auto-generated name.
     *
     * @param props the actor Props
     * @param <T> the actor type
     * @return the TestActorRef
     */
    @SuppressWarnings("unchecked")
    public <T extends Actor> TestActorRef<T> createTestActor(final Props props) {
        TestActorRef<T> actorRef = TestActorRef.create(system, props);
        return (TestActorRef<T>) addActor(actorRef, true);
    }

    private <T extends ActorRef> ActorRef addActor(final T actorRef, final boolean verify) {
        createdActors.add(actorRef);
        if (verify) {
            verifyActorReady(actorRef);
        }

        return actorRef;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void verifyActorReady(final ActorRef actorRef) {
        // Sometimes we see messages go to dead letters soon after creation - it seems the actor isn't quite
        // in a state yet to receive messages or isn't actually created yet. This seems to happen with
        // actorSelection so, to alleviate it, we use an actorSelection and send an Identify message with
        // retries to ensure it's ready.

        Timeout timeout = new Timeout(100, TimeUnit.MILLISECONDS);
        Throwable lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            try {
                ActorSelection actorSelection = system.actorSelection(actorRef.path().toString());
                Future<Object> future = Patterns.ask(actorSelection, new Identify(""), timeout);
                ActorIdentity reply = (ActorIdentity)Await.result(future, timeout.duration());
                assertTrue("Identify returned non-present", reply.getActorRef().isPresent());
                return;
            } catch (Exception | AssertionError e) {
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                lastError = e;
            }
        }

        throw new RuntimeException(lastError);
    }

    /**
     * Generate a friendly but unique actor id/name.
     *
     * @param prefix the name prefix
     * @return the actor name
     */
    public String generateActorId(final String prefix) {
        return prefix + actorCount++;
    }

    public void killActor(final ActorRef actor, final TestKit kit) {
        killActor(actor, kit, true);
    }

    private void killActor(final ActorRef actor, final TestKit kit, final boolean remove) {
        LOG.info("Killing actor {}", actor);
        kit.watch(actor);
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        kit.expectTerminated(Duration.ofSeconds(5), actor);

        if (remove) {
            createdActors.remove(actor);
        }
    }

    public String createTestActorPath(final String actorId) {
        return "pekko://test/user/" + actorId;
    }

    @Override
    public void close() {
        TestKit kit = new TestKit(system);
        for (ActorRef actor : createdActors) {
            killActor(actor, kit, false);
        }
    }
}
