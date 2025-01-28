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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opendaylight.controller.cluster.databroker.TestClientBackedDataStore;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

@RunWith(Parameterized.class)
public class DistributedDataStoreWithSegmentedJournalIntegrationTest
        extends AbstractDistributedDataStoreIntegrationTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { TestClientBackedDataStore.class }
        });
    }

    @Before
    public void setUp() {
        InMemorySnapshotStore.clear();
        system = ActorSystem.create("cluster-test",
                ConfigFactory.load("segmented.conf").getConfig("Member1"));
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
        File journalDir = new File(system.settings().config()
                .getString("pekko.persistence.journal.segmented-file.root-directory"));

        if (!journalDir.exists()) {
            return;
        }

        try {
            FileUtils.cleanDirectory(journalDir);
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void testManyWritesDeletes() throws Exception {
        final var testKit = new IntegrationTestKit(stateDir(), getSystem(), datastoreContextBuilder);
        CollectionNodeBuilder<MapEntryNode, SystemMapNode> carMapBuilder = ImmutableNodes.mapNodeBuilder(CAR_QNAME);

        try (var dataStore = testKit.setupDataStore(testParameter, "testManyWritesDeletes",
            "module-shards-cars-member-1.conf", true, "cars")) {

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            testKit.doCommit(writeTx.ready());

            int numCars = 20;
            for (int i = 0; i < numCars; ++i) {
                DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                YangInstanceIdentifier path = CarsModel.newCarPath("car" + i);
                MapEntryNode data = CarsModel.newCarEntry("car" + i, Uint64.valueOf(20000));

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

            final Optional<NormalizedNode> optional = txChain.newReadOnlyTransaction()
                    .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());

            MapNode cars = carMapBuilder.build();

            assertEquals("cars not matching result", cars, optional.orElseThrow());

            txChain.close();


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
        try (var dataStore = testKit.setupDataStore(testParameter, "testManyWritesDeletes",
            "module-shards-cars-member-1.conf", true, "cars")) {

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();
            MapNode cars = carMapBuilder.build();

            final Optional<NormalizedNode> optional = txChain.newReadOnlyTransaction()
                    .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("restored cars do not match snapshot", Optional.of(cars), optional);

            txChain.close();
        }
    }
}
