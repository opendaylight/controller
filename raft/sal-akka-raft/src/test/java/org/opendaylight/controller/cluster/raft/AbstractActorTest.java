/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.apache.commons.io.FileUtils;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

public abstract class AbstractActorTest {
    protected static final class MockIdentifier extends AbstractStringIdentifier<MockIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        public MockIdentifier(final String string) {
            super(string);
        }
    }

    private static ActorSystem ACTOR_SYSTEM;

    // FIXME: @TempDir when we have JUnit5
    private Path stateDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        deleteJournal();
        System.setProperty("shard.persistent", "false");
        ACTOR_SYSTEM = ActorSystem.create("test");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        deleteJournal();
        TestKit.shutdownActorSystem(ACTOR_SYSTEM);
        ACTOR_SYSTEM = null;
    }

    @Before
    public void beforeEach() throws Exception {
        stateDir = Files.createTempDirectory(getClass().getName());
    }

    @After
    public void afterEach() throws Exception {
        try (var paths = Files.walk(stateDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    protected static final ActorSystem getSystem() {
        return ACTOR_SYSTEM;
    }

    protected final Path stateDir() {
        return requireNonNull(stateDir);
    }

    protected static void deleteJournal() throws IOException {
        FileUtils.deleteDirectory(Path.of("journal").toFile());
    }
}
