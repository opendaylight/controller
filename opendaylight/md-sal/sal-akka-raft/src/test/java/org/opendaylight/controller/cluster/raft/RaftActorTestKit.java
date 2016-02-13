/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class RaftActorTestKit extends JavaTestKit {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorTestKit.class);
    private final ActorRef raftActor;

    public RaftActorTestKit(ActorSystem actorSystem, String actorName) {
        super(actorSystem);

        raftActor = this.getSystem().actorOf(MockRaftActor.builder().id(actorName).props(), actorName);

    }


    public ActorRef getRaftActor() {
        return raftActor;
    }

    public boolean waitForLogMessage(final Class<?> logEventClass, String message){
        // Wait for a specific log message to show up
        return
            new JavaTestKit.EventFilter<Boolean>(logEventClass
            ) {
                @Override
                protected Boolean run() {
                    return true;
                }
            }.from(raftActor.path().toString())
                .message(message)
                .occurrences(1).exec();


    }

    protected void waitUntilLeader(){
        waitUntilLeader(raftActor);
    }

    public static void waitUntilLeader(ActorRef actorRef) {
        FiniteDuration duration = Duration.create(100, TimeUnit.MILLISECONDS);
        for(int i = 0; i < 20 * 5; i++) {
            Future<Object> future = Patterns.ask(actorRef, FindLeader.INSTANCE, new Timeout(duration));
            try {
                final Optional<String> maybeLeader = ((FindLeaderReply)Await.result(future, duration)).getLeaderActor();
                if (maybeLeader.isPresent()) {
                    return;
                }
            } catch(TimeoutException e) {
            } catch(Exception e) {
                LOG.error("FindLeader failed", e);
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        Assert.fail("Leader not found for actorRef " + actorRef.path());
    }

}