/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Verify.verifyNotNull;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.awaitility.core.ConditionFactory;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

// FIXME: document this class
public class RaftActorTestKit extends TestKit {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorTestKit.class);

    private final @NonNull ActorRef raftActor;

    public RaftActorTestKit(final Path stateDir, final ActorSystem actorSystem, final String actorName) {
        super(actorSystem);
        raftActor = verifyNotNull(getSystem().actorOf(MockRaftActor.builder()
            .id(actorName)
            .props(stateDir), actorName));
    }

    public final @NonNull ActorRef getRaftActor() {
        return raftActor;
    }

    protected void waitUntilLeader() {
        waitUntilLeader(raftActor);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static final void waitUntilLeader(final ActorRef actorRef) {
        // FIXME: use awaitility here
        final var duration = FiniteDuration.create(100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 20 * 5; i++) {
            final var future = Patterns.ask(actorRef, FindLeader.INSTANCE, new Timeout(duration));
            try {
                final var reply = assertInstanceOf(FindLeaderReply.class, Await.result(future, duration));
                if (reply.leaderActorPath() != null) {
                    return;
                }
            } catch (Exception e) {
                LOG.error("FindLeader failed", e);
            }

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        throw new AssertionError("Leader not found for actorRef " + actorRef.path());
    }

    public static final @NonNull EntryJournal assertJournal(final RaftActor actor) {
        return assertInstanceOf(EnabledRaftStorage.class, actor.persistence().entryStore()).journal();
    }

    public static final @NonNull EntryJournal assertJournal(final TestActorRef<? extends RaftActor> actor) {
        return assertJournal(actor.underlyingActor());
    }

    public static final void awaitLastApplied(final RaftActor actor, final long lastApplied) {
        defaultAwait().untilAsserted(() -> {
            assertEquals(lastApplied, actor.getRaftActorContext().getReplicatedLog().getLastApplied());
        });
    }

    public static final void awaitLastApplied(final TestActorRef<? extends RaftActor> actor, final long lastApplied) {
        awaitLastApplied(actor.underlyingActor(), lastApplied);
    }

    public static final @NonNull SnapshotFile awaitSnapshot(final RaftActor actor) {
        final var snapshot = defaultAwait()
            .until(() -> actor.persistence().snapshotStore().lastSnapshot(), Objects::nonNull);
        assertNotNull(snapshot);
        return snapshot;
    }

    public static final @NonNull SnapshotFile awaitSnapshot(final TestActorRef<? extends RaftActor> actorRef) {
        return awaitSnapshot(actorRef.underlyingActor());
    }

    public static final @NonNull SnapshotFile awaitSnapshotNewerThan(final TestActorRef<? extends RaftActor> actorRef,
            final Instant timestamp) {
        return awaitSnapshotNewerThan(actorRef.underlyingActor(), timestamp);
    }

    public static final @NonNull SnapshotFile awaitSnapshotNewerThan(final RaftActor actor, final Instant timestamp) {
        return defaultAwait()
            .until(() -> awaitSnapshot(actor), snapshot -> timestamp.compareTo(snapshot.timestamp()) < 0);
    }

    // Wait for 5 seconds mostly due to slow build machines (like Vexxhost or on a single-threaded control group.
    // Other operations execute on a similar or smaller scale, so let's keep this unified.
    private static ConditionFactory defaultAwait() {
        return await().atMost(Duration.ofSeconds(5));
    }
}
