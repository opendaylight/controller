/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.apache.pekko.actor.ActorRef.noSender;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.EventFilter;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
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
                final var maybeLeader = ((FindLeaderReply) Await.result(future, duration)).leaderActorPath();
                if (maybeLeader != null) {
                    return maybeLeader;
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
                final var maybeLeader = ((FindLeaderReply) Await.result(future, duration)).leaderActorPath();
                if (maybeLeader == null) {
                    return;
                }

                lastResponse = maybeLeader;
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

    public @NonNull ShardTestConnection connect(final TestActorRef<Shard> shard, final ClientIdentifier clientId) {
        return connect(shard, clientId, Duration.ofSeconds(5));
    }

    public @NonNull ShardTestConnection connect(final TestActorRef<Shard> shard, final ClientIdentifier clientId,
            final Duration max) {
        shard.tell(new ConnectClientRequest(clientId, getRef(), ABIVersion.current(), ABIVersion.current()),
            noSender());
        final var connect = expectMsgClass(max, ConnectClientSuccess.class);
        assertEquals(clientId, connect.getTarget());
        assertSame(shard, connect.getBackend());
        return new ShardTestConnection(this, shard, clientId, connect);
    }
}
