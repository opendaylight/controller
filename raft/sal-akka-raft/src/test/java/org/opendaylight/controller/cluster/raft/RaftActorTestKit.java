/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.fail;

import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.javadsl.EventFilter;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class RaftActorTestKit extends TestKit {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorTestKit.class);
    private final ActorRef raftActor;

    public RaftActorTestKit(final Path stateDir, final ActorSystem actorSystem, final String actorName) {
        super(actorSystem);
        raftActor = getSystem().actorOf(MockRaftActor.builder().id(actorName).props(stateDir), actorName);
    }

    public ActorRef getRaftActor() {
        return raftActor;
    }

    public boolean waitForLogMessage(final Class<?> logEventClass, final String message) {
        // Wait for a specific log message to show up
        return new EventFilter(logEventClass, getSystem()).from(raftActor.path().toString()).message(message)
                .occurrences(1).intercept(() -> Boolean.TRUE);
    }

    protected void waitUntilLeader() {
        waitUntilLeader(raftActor);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void waitUntilLeader(final ActorRef actorRef) {
        FiniteDuration duration = FiniteDuration.create(100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 20 * 5; i++) {
            Future<Object> future = Patterns.ask(actorRef, FindLeader.INSTANCE, new Timeout(duration));
            try {
                final Optional<String> maybeLeader = ((FindLeaderReply)Await.result(future, duration)).getLeaderActor();
                if (maybeLeader.isPresent()) {
                    return;
                }
            } catch (Exception e) {
                LOG.error("FindLeader failed", e);
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        fail("Leader not found for actorRef " + actorRef.path());
    }
}
