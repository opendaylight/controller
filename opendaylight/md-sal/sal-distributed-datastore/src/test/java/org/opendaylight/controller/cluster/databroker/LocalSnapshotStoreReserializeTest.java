/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.persistence.Persistence;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SelectedSnapshot;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotProtocol.LoadSnapshot;
import akka.persistence.SnapshotProtocol.LoadSnapshotResult;
import akka.persistence.SnapshotProtocol.SaveSnapshot;
import akka.persistence.SnapshotSelectionCriteria;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Stopwatch;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

/**
 * Unit tests for LocalSnapshotStore. These are in addition to LocalSnapshotStoreSpecTest to cover a few cases
 * that SnapshotStoreSpec doesn't.
 *
 * @author Thomas Pantelis
 */
public class LocalSnapshotStoreReserializeTest {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSnapshotStoreReserializeTest.class);
    private static final String PERSISTENCE_ID = "member-1-shard-default-config";

    private static ActorSystem system;
    private static ActorRef snapshotStore;

    private long seqNr = 1340346L;
    private long timestamp = 1558405305800L;

    @BeforeClass
    public static void staticSetup() {
        system = ActorSystem.create("test", ConfigFactory.load("LocalSnapshotStoreTest2.conf"));
        snapshotStore = system.registerExtension(Persistence.lookup())
                .snapshotStoreFor(null, ConfigFactory.empty());
    }


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void reserialize() {
        SnapshotMetadata metadata3 = new SnapshotMetadata(PERSISTENCE_ID, seqNr, timestamp);

        Stopwatch stopwatch = Stopwatch.createStarted();

        TestKit probe = new TestKit(system);
        snapshotStore.tell(new LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        LoadSnapshotResult result = probe.expectMsgClass(Duration.ofSeconds(600), LoadSnapshotResult.class);
        Option<SelectedSnapshot> possibleSnapshot = result.snapshot();

        long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        LOG.warn("Load took {} seconds", elapsed);

        SaveSnapshot saveSnapshot = new SaveSnapshot(new SnapshotMetadata(PERSISTENCE_ID, seqNr,
                timestamp + 1), possibleSnapshot.get().snapshot());

        snapshotStore.tell(saveSnapshot, probe.getRef());
        SaveSnapshotSuccess saveResult = probe.expectMsgClass(Duration.ofSeconds(600), SaveSnapshotSuccess.class);
    }

    @Test
    public void loadSnapshot() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();

        TestKit probe = new TestKit(system);
        snapshotStore.tell(new LoadSnapshot(PERSISTENCE_ID,
                SnapshotSelectionCriteria.latest(), Long.MAX_VALUE), probe.getRef());
        LoadSnapshotResult result = probe.expectMsgClass(Duration.ofSeconds(600), LoadSnapshotResult.class);
        Option<SelectedSnapshot> possibleSnapshot = result.snapshot();

        long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        LOG.warn("Load took {} seconds", elapsed);
    }
}
