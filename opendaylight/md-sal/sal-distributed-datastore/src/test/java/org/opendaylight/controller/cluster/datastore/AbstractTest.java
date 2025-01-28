/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractTest {
    protected static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    protected static final MemberName MEMBER_2_NAME = MemberName.forName("member-2");

    // FIXME: use a different name
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("ShardTransactionTest");

    protected static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);

    protected static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);

    private static final @NonNull LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0);
    private static final AtomicLong HISTORY_COUNTER = new AtomicLong();
    private static final AtomicLong TX_COUNTER = new AtomicLong();

    private final ArrayList<ActorSystem> actorSystems = new ArrayList<>();

    // FIXME: @TempDir when we have JUnit5
    private Path stateDir;

    protected static void setUpStatic() {
        HISTORY_COUNTER.set(1L);
        TX_COUNTER.set(1L);
    }

    protected static @NonNull TransactionIdentifier newTransactionId(final long txId) {
        return new TransactionIdentifier(HISTORY_ID, txId);
    }

    protected static @NonNull TransactionIdentifier nextTransactionId() {
        return newTransactionId(TX_COUNTER.getAndIncrement());
    }

    protected static LocalHistoryIdentifier newHistoryId(final long historyId) {
        return new LocalHistoryIdentifier(CLIENT_ID, historyId);
    }

    protected static LocalHistoryIdentifier nextHistoryId() {
        return newHistoryId(HISTORY_COUNTER.incrementAndGet());
    }

    @Before
    public final void setupStateDir() throws Exception {
        stateDir = Files.createTempDirectory(getClass().getName());
    }

    @After
    public void actorSystemCleanup() throws Exception {
        for (var system : actorSystems) {
            TestKit.shutdownActorSystem(system, true);
        }
        try (var paths = Files.walk(stateDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    protected final Path stateDir() {
        return stateDir;
    }

    protected ActorSystem newActorSystem(final String name, final String config) {
        final var system = ActorSystem.create(name, ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }
}
