/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

public abstract class AbstractActorTest {
    protected static final class MockIdentifier extends AbstractStringIdentifier<MockIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        public MockIdentifier(final String string) {
            super(string);
        }
    }

    protected static final @NonNull RestrictedObjectStreams OBJECT_STREAMS =
        RestrictedObjectStreams.ofClassLoaders(AbstractRaftActorIntegrationTest.class);

    private static ActorSystem ACTOR_SYSTEM;

    @TempDir
    private Path stateDir;

    @BeforeAll
    public static final void beforeAll() throws Exception {
        deleteJournal();
        System.setProperty("shard.persistent", "false");
        ACTOR_SYSTEM = ActorSystem.create("test");
    }

    @AfterAll
    public static final void afterAll() throws Exception {
        deleteJournal();
        TestKit.shutdownActorSystem(ACTOR_SYSTEM);
        ACTOR_SYSTEM = null;
    }

    protected static final ActorSystem getSystem() {
        return ACTOR_SYSTEM;
    }

    protected final Path stateDir() {
        return requireNonNull(stateDir);
    }

    protected static final void deleteJournal() throws IOException {
        FileUtils.deleteDirectory(Path.of("journal").toFile());
    }
}
