/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.cluster.datastore.model.CarsModel.CAR_QNAME;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.AddressFromURIString;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

public class DistributedDataStoreWithSegmentedJournalIntegrationTest
        extends AbstractDistributedDataStoreIntegrationTest {
    @Before
    public void setUp() {
        system = ActorSystem.create("cluster-test", ConfigFactory.load("segmented.conf").getConfig("Member1"));
        cleanSnapshotDir(system);

        Address member1Address = AddressFromURIString.parse("pekko://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, true);
        system = null;
    }

    private static void cleanSnapshotDir(final ActorSystem system) {
        final var journalDir = Path.of(system.settings().config()
                .getString("pekko.persistence.journal.segmented-file.root-directory"));
        if (Files.exists(journalDir)) {
            try {
                FileUtils.cleanDirectory(journalDir.toFile());
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Test
    public void testManyWritesDeletes() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        final var carMapBuilder = ImmutableNodes.newSystemMapBuilder()
            .withNodeIdentifier(new NodeIdentifier(CAR_QNAME));

        try (var dataStore = testKit.setupDataStore(DS_CLASS, "testManyWritesDeletes",
            "module-shards-cars-member-1.conf", true, "cars")) {

            try (var txChain = dataStore.createTransactionChain()) {
                final var writeTx = txChain.newWriteOnlyTransaction();
                writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
                testKit.doCommit(writeTx.ready());

                int numCars = 20;
                for (int i = 0; i < numCars; ++i) {
                    var rwTx = txChain.newReadWriteTransaction();

                    final var path = CarsModel.newCarPath("car" + i);
                    final var data = CarsModel.newCarEntry("car" + i, Uint64.valueOf(20000));

                    rwTx.merge(path, data);
                    carMapBuilder.withChild(data);

                    testKit.doCommit(rwTx.ready());

                    if (i % 5 == 0) {
                        rwTx = txChain.newReadWriteTransaction();

                        rwTx.delete(path);
                        carMapBuilder.withoutChild(path.getLastPathArgument());
                        testKit.doCommit(rwTx.ready());
                    }
                }

                try (var readTx = txChain.newReadOnlyTransaction()) {
                    assertEquals("cars not matching result", Optional.of(carMapBuilder.build()),
                        readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS));
                }
            }


            // wait until the journal is actually persisted, killing the datastore early results in missing entries
            Stopwatch sw = Stopwatch.createStarted();
            AtomicBoolean done = new AtomicBoolean(false);
            while (!done.get()) {
                MemberNode.verifyRaftState(dataStore, "cars", raftState -> {
                    if (raftState.getLastApplied() == raftState.getLastLogIndex()) {
                        done.set(true);
                    }
                });

                assertTrue("Shard did not persist all journal entries in time.", sw.elapsed(TimeUnit.SECONDS) <= 5);

                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        // test restoration from journal and verify data matches
        try (var dataStore = testKit.setupDataStore(DS_CLASS, "testManyWritesDeletes",
            "module-shards-cars-member-1.conf", true, "cars")) {

            try (var txChain = dataStore.createTransactionChain()) {
                try (var readTx = txChain.newReadOnlyTransaction()) {
                    assertEquals("restored cars do not match snapshot", Optional.of(carMapBuilder.build()),
                        readTx.read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS));
                }
            }
        }
    }
}
