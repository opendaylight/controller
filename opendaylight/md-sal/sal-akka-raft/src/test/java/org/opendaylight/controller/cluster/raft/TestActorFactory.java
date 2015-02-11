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
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestActorFactory provides methods to create both normal and test actors and to kill them when the factory is closed
 * The ideal usage for TestActorFactory is with try with resources, <br/>
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
    private final ActorSystem system;
    List<ActorRef> createdActors = new LinkedList<>();
    Logger LOG = LoggerFactory.getLogger(getClass());
    private static int actorCount = 1;

    public TestActorFactory(ActorSystem system){
        this.system = system;
    }

    /**
     * Create a normal actor with an auto-generated name
     *
     * @param props
     * @return
     */
    public ActorRef createActor(Props props){
        ActorRef actorRef = system.actorOf(props);
        createdActors.add(actorRef);
        return actorRef;
    }

    /**
     * Create a normal actor with the passed in name
     * @param props
     * @param actorId name of actor
     * @return
     */
    public ActorRef createActor(Props props, String actorId){
        ActorRef actorRef = system.actorOf(props, actorId);
        createdActors.add(actorRef);
        return actorRef;
    }

    /**
     * Create a test actor with the passed in name
     * @param props
     * @param actorId
     * @param <T>
     * @return
     */
    public <T extends Actor> TestActorRef<T> createTestActor(Props props, String actorId){
        TestActorRef<T> actorRef = TestActorRef.create(system, props, actorId);
        createdActors.add(actorRef);
        return actorRef;
    }

    /**
     * Create a test actor with an auto-generated name
     * @param props
     * @param <T>
     * @return
     */
    public <T extends Actor> TestActorRef<T> createTestActor(Props props){
        TestActorRef<T> actorRef = TestActorRef.create(system, props);
        createdActors.add(actorRef);
        return actorRef;
    }

    public ActorRef createActor(Class<?> actorClass, String actorId) {
        ActorRef actorRef = system.actorOf(Props.create(actorClass), actorId);
        createdActors.add(actorRef);
        return actorRef;
    }

    public void addActor(ActorRef actorRef) {
        createdActors.add(actorRef);
    }

    /**
     * Generate a friendly but unique actor id/name
     * @param prefix
     * @return
     */
    public String generateActorId(String prefix){
        return prefix + actorCount++;
    }

    @Override
    public void close() {
        new JavaTestKit(system) {{
            for(ActorRef actor : createdActors) {
                watch(actor);
                LOG.info("Killing actor {}", actor);
                actor.tell(PoisonPill.getInstance(), ActorRef.noSender());
                expectTerminated(duration("5 seconds"), actor);
            }
        }};
    }
}