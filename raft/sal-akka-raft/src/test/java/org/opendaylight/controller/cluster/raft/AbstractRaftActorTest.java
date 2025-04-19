/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;
import org.apache.pekko.testkit.TestActorRef;

abstract class AbstractRaftActorTest extends AbstractActorTest {
    static final void awaitSnapshot(final RaftActor actor) {
        final var stateDir = actor.localAccess().stateDir();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            try (var stream = Files.list(stateDir)) {
                assertTrue(stream
                    .map(path -> path.getFileName().toString())
                    .anyMatch(str -> str.startsWith("snapshot-") && str.endsWith(".v1")));
            }
        });
    }

    static final void awaitSnapshot(final TestActorRef<? extends RaftActor> actorRef) {
        awaitSnapshot(actorRef.underlyingActor());
    }
}
