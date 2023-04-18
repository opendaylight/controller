/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.testkit.javadsl.EventFilter;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ShardTestKit extends TestKit {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTestKit.class);

    public ShardTestKit(final ActorSystem actorSystem) {
        super(actorSystem);
    }

    public void waitForLogMessage(final Class<?> logLevel, final ActorRef subject, final String logMessage) {
        // Wait for a specific log message to show up
        final Boolean result = new EventFilter(logLevel, getSystem()).from(subject.path().toString())
                .message(logMessage).occurrences(1).intercept(() -> Boolean.TRUE);
        assertEquals(Boolean.TRUE, result);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static String waitUntilLeader(final ActorRef shard) {
        FiniteDuration duration = FiniteDuration.create(100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 20 * 5; i++) {
            Future<Object> future = Patterns.ask(shard, FindLeader.INSTANCE, new Timeout(duration));
            try {
                final var maybeLeader = ((FindLeaderReply) Await.result(future, duration)).getLeaderActor();
                if (maybeLeader.isPresent()) {
                    return maybeLeader.orElseThrow();
                }
            } catch (TimeoutException e) {
                LOG.trace("FindLeader timed out", e);
            } catch (Exception e) {
                LOG.error("FindLeader failed", e);
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        fail("Leader not found for shard " + shard.path());
        return null;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void waitUntilNoLeader(final ActorRef shard) {
        FiniteDuration duration = FiniteDuration.create(100, TimeUnit.MILLISECONDS);
        Object lastResponse = null;
        for (int i = 0; i < 20 * 5; i++) {
            Future<Object> future = Patterns.ask(shard, FindLeader.INSTANCE, new Timeout(duration));
            try {
                final var maybeLeader = ((FindLeaderReply) Await.result(future, duration)).getLeaderActor();
                if (!maybeLeader.isPresent()) {
                    return;
                }

                lastResponse = maybeLeader.orElseThrow();
            } catch (TimeoutException e) {
                lastResponse = e;
            } catch (Exception e) {
                LOG.error("FindLeader failed", e);
                lastResponse = e;
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        if (lastResponse instanceof Throwable) {
            throw (AssertionError)new AssertionError(
                    String.format("Unexpected error occurred from FindLeader for shard %s", shard.path()))
                            .initCause((Throwable)lastResponse);
        }

        fail(String.format("Unexpected leader %s found for shard %s", lastResponse, shard.path()));
    }
}
