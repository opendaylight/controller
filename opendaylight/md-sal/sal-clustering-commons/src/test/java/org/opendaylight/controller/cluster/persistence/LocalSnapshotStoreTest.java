/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opendaylight.controller.cluster.persistence.LocalSnapshotStoreSpecTest.SNAPSHOT_DIR;
import static org.opendaylight.controller.cluster.persistence.LocalSnapshotStoreSpecTest.cleanSnapshotDir;
import static org.opendaylight.controller.cluster.persistence.LocalSnapshotStoreSpecTest.createSnapshotDir;

import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.persistence.Persistence;
import org.apache.pekko.persistence.SnapshotMetadata;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotProtocol.LoadSnapshot;
import org.apache.pekko.persistence.SnapshotProtocol.LoadSnapshotFailed;
import org.apache.pekko.persistence.SnapshotProtocol.LoadSnapshotResult;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.apache.pekko.persistence.serialization.Snapshot;
import org.apache.pekko.persistence.serialization.SnapshotSerializer;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for LocalSnapshotStore. These are in addition to LocalSnapshotStoreSpecTest to cover a few cases
 * that SnapshotStoreSpec doesn't.
 *
 * @author Thomas Pantelis
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@Deprecated(since = "11.0.0", forRemoval = true)
public class LocalSnapshotStoreTest {
    private static final String PERSISTENCE_ID = "member-1-shard-default-config";
    private static final String PREFIX_BASED_SHARD_PERSISTENCE_ID = "member-1-shard-id-ints!-config";

    private static ActorSystem system;
    private static ActorRef snapshotStore;

    @BeforeClass
    public static void staticSetup() {
        createSnapshotDir();

        system = ActorSystem.create("test", ConfigFactory.load("LocalSnapshotStoreTest.conf"));
        snapshotStore = ((Persistence) system.registerExtension(Persistence.lookup()))
            .snapshotStoreFor(null, ConfigFactory.empty());
    }

    @AfterClass
    public static void staticCleanup() {
        FileUtils.deleteQuietly(SNAPSHOT_DIR.toFile());
        TestKit.shutdownActorSystem(system);
    }

    @Before
    public void setup() {
        cleanSnapshotDir();
    }

    @After
    public void cleanup() {
        cleanSnapshotDir();
    }

    @Test
    public void testDoLoadAsync() throws IOException {
        createSnapshotFile(PERSISTENCE_ID, "one", 0, 1000);
        createSnapshotFile(PERSISTENCE_ID, "two", 1, 2000);
        createSnapshotFile(PERSISTENCE_ID, "three", 1, 3000);

        createSnapshotFile(PREFIX_BASED_SHARD_PERSISTENCE_ID, "foo", 0, 1000);
        createSnapshotFile(PREFIX_BASED_SHARD_PERSISTENCE_ID, "bar", 1, 2000);
        createSnapshotFile(PREFIX_BASED_SHARD_PERSISTENCE_ID, "foobar", 1, 3000);

        createSnapshotFile("member-1-shard-default-oper", "foo", 0, 1000);
        createSnapshotFile("member-1-shard-toaster-oper", "foo", 0, 1000);
        Files.createFile(SNAPSHOT_DIR.resolve("other"));
        Files.createFile(SNAPSHOT_DIR.resolve("other-1485349217290"));

        SnapshotMetadata metadata3 = new SnapshotMetadata(PERSISTENCE_ID, 1, 3000);

        TestKit probe = new TestKit(system);
        snapshotStore.tell(new LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        var result = probe.expectMsgClass(LoadSnapshotResult.class);
        var possibleSnapshot = result.snapshot();

        assertTrue("SelectedSnapshot present", possibleSnapshot.nonEmpty());
        assertEquals("SelectedSnapshot metadata", metadata3, possibleSnapshot.get().metadata());
        assertEquals("SelectedSnapshot snapshot", "three", possibleSnapshot.get().snapshot());

        snapshotStore.tell(new LoadSnapshot(PREFIX_BASED_SHARD_PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        result = probe.expectMsgClass(LoadSnapshotResult.class);
        possibleSnapshot = result.snapshot();

        SnapshotMetadata prefixBasedShardMetada3 = new SnapshotMetadata(PREFIX_BASED_SHARD_PERSISTENCE_ID, 1, 3000);

        assertTrue("SelectedSnapshot present", possibleSnapshot.nonEmpty());
        assertEquals("SelectedSnapshot metadata", prefixBasedShardMetada3, possibleSnapshot.get().metadata());
        assertEquals("SelectedSnapshot snapshot", "foobar", possibleSnapshot.get().snapshot());
    }

    @Test
    public void testDoLoadAsyncWithNoSnapshots() {
        TestKit probe = new TestKit(system);
        snapshotStore.tell(new LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        final var result = probe.expectMsgClass(LoadSnapshotResult.class);
        final var possibleSnapshot = result.snapshot();

        assertFalse("SelectedSnapshot present", possibleSnapshot.nonEmpty());
    }

    @Test
    public void testDoLoadAsyncWithRetry() throws IOException  {
        createSnapshotFile(PERSISTENCE_ID, "one", 0, 1000);
        createSnapshotFile(PERSISTENCE_ID, null, 1, 2000);

        SnapshotMetadata metadata = new SnapshotMetadata(PERSISTENCE_ID, 0, 1000);

        TestKit probe = new TestKit(system);
        snapshotStore.tell(new LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        final var result = probe.expectMsgClass(LoadSnapshotResult.class);
        final var possibleSnapshot = result.snapshot();

        assertTrue("SelectedSnapshot present", possibleSnapshot.nonEmpty());
        assertEquals("SelectedSnapshot metadata", metadata, possibleSnapshot.get().metadata());
        assertEquals("SelectedSnapshot snapshot", "one", possibleSnapshot.get().snapshot());
    }

    @Test
    public void testDoLoadAsyncWithFailure() throws Exception {
        createSnapshotFile(PERSISTENCE_ID, null, 1, 2000);
        TestKit probe = new TestKit(system);
        snapshotStore.tell(new SnapshotProtocol.LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());

        final var failed = probe.expectMsgClass(LoadSnapshotFailed.class);
        assertInstanceOf(IOException.class, failed.cause());
    }

    @Test
    public void testDoLoadAsyncWithAkkaSerializedSnapshot() throws IOException {
        SnapshotSerializer snapshotSerializer = new SnapshotSerializer((ExtendedActorSystem) system);

        String name = toSnapshotName(PERSISTENCE_ID, 1, 1000);
        try (var fos = Files.newOutputStream(SNAPSHOT_DIR.resolve(name))) {
            fos.write(snapshotSerializer.toBinary(new Snapshot("one")));
        }

        SnapshotMetadata metadata = new SnapshotMetadata(PERSISTENCE_ID, 1, 1000);

        TestKit probe = new TestKit(system);
        snapshotStore.tell(new LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        final var result = probe.expectMsgClass(LoadSnapshotResult.class);
        final var possibleSnapshot = result.snapshot();

        assertTrue("SelectedSnapshot present", possibleSnapshot.nonEmpty());
        assertEquals("SelectedSnapshot metadata", metadata, possibleSnapshot.get().metadata());
        assertEquals("SelectedSnapshot snapshot", "one", possibleSnapshot.get().snapshot());
    }

    private static void createSnapshotFile(final String persistenceId, final String payload, final int seqNr,
            final int timestamp) throws IOException {
        String name = toSnapshotName(persistenceId, seqNr, timestamp);
        try (var fos = Files.newOutputStream(SNAPSHOT_DIR.resolve(name))) {
            if (payload != null) {
                fos.write(SerializationUtils.serialize(payload));
            }
        }
    }

    private static String toSnapshotName(final String persistenceId, final int seqNr, final int timestamp) {
        return "snapshot-" + URLEncoder.encode(persistenceId, StandardCharsets.UTF_8) + "-" + seqNr + "-" + timestamp;
    }
}
