/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.apache.pekko.testkit.TestActorRef;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;

abstract class AbstractRaftActorTest extends AbstractActorTest {
    static final SnapshotFile awaitSnapshot(final RaftActor<?> actor) {
        return await().atMost(Duration.ofSeconds(5))
            .until(() -> actor.persistence().lastSnapshot(), Objects::nonNull);
    }

    static final SnapshotFile awaitSnapshot(final TestActorRef<? extends RaftActor<?>> actorRef) {
        return awaitSnapshot(actorRef.underlyingActor());
    }

    static final SnapshotFile awaitSnapshotNewerThan(final TestActorRef<? extends RaftActor<?>> actorRef,
            final Instant timestamp) {
        return await().atMost(Duration.ofSeconds(5))
            .until(() -> awaitSnapshot(actorRef), snapshot -> timestamp.compareTo(snapshot.timestamp()) < 0);
    }
}
