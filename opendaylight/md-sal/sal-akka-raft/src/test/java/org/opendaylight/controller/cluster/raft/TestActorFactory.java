/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import akka.actor.Actor;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.actor.InvalidActorNameException;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
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
    List<ActorRef> createdActors = new LinkedList<>();
    private static int actorCount = 1;

    public TestActorFactory(ActorSystem system) {
        this.system = system;
    }

    /**
     * Create a normal actor with an auto-generated name.
     *
     * @param props the actor Props
     * @return the ActorRef
     */
    public ActorRef createActor(Props props) {
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
    public ActorRef createActor(Props props, String actorId) {
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
    public ActorRef createActorNoVerify(Props props, String actorId) {
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
    public <T extends Actor> TestActorRef<T> createTestActor(Props props, String actorId) {
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
    public <T extends Actor> TestActorRef<T> createTestActor(Props props) {
        TestActorRef<T> actorRef = TestActorRef.create(system, props);
        return (TestActorRef<T>) addActor(actorRef, true);
    }

    private <T extends ActorRef> ActorRef addActor(T actorRef, boolean verify) {
        createdActors.add(actorRef);
        if (verify) {
            verifyActorReady(actorRef);
        }

        return actorRef;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void verifyActorReady(ActorRef actorRef) {
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
                Assert.assertNotNull("Identify returned null", reply.getRef());
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
    public String generateActorId(String prefix) {
        return prefix + actorCount++;
    }

    public void killActor(ActorRef actor, JavaTestKit kit) {
        killActor(actor, kit, true);
    }

    private void killActor(ActorRef actor, JavaTestKit kit, boolean remove) {
        LOG.info("Killing actor {}", actor);
        kit.watch(actor);
        actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        kit.expectTerminated(JavaTestKit.duration("5 seconds"), actor);

        if (remove) {
            createdActors.remove(actor);
        }
    }

    public String createTestActorPath(String actorId) {
        return "akka://test/user/" + actorId;
    }

    @Override
    public void close() {
        JavaTestKit kit = new JavaTestKit(system);
        for (ActorRef actor : createdActors) {
            killActor(actor, kit, false);
        }
    }
}
