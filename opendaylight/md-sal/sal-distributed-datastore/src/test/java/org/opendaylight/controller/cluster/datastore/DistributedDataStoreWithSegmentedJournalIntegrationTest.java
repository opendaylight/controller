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

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;

@RunWith(Parameterized.class)
public class DistributedDataStoreWithSegmentedJournalIntegrationTest
        extends AbstractDistributedDataStoreIntegrationTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { TestDistributedDataStore.class }, { TestClientBackedDataStore.class }
        });
    }

    @Before
    public void setUp() {
        InMemorySnapshotStore.clear();
        system = ActorSystem.create("cluster-test",
                ConfigFactory.load("segmented.conf").getConfig("Member1"));
        cleanSnapshotDir(system);

        Address member1Address = AddressFromURIString.parse("akka://cluster-test@127.0.0.1:2558");
        Cluster.get(system).join(member1Address);
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system, true);
        system = null;
    }

    private static void cleanSnapshotDir(final ActorSystem system) {
        File journalDir = new File(system.settings().config()
                .getString("akka.persistence.journal.segmented-file.root-directory"));

        if (!journalDir.exists()) {
            return;
        }

        try {
            FileUtils.cleanDirectory(journalDir);
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Generate cars with name = [namePrefix+index] where index is number from 0 to amount-1.
     */
    private Collection<Pair<YangInstanceIdentifier, MapEntryNode>> generateCars(final int amount,
        final String namePrefix) {
        LinkedList<Pair<YangInstanceIdentifier, MapEntryNode>> cars = new LinkedList<>();
        for (int i = 0; i < amount; ++i) {
            YangInstanceIdentifier path = CarsModel.newCarPath(namePrefix + i);
            MapEntryNode data = CarsModel.newCarEntry(namePrefix + i, Uint64.valueOf(20000));
            cars.add(Pair.of(path, data));
        }
        return cars;
    }

    @Test
    public void testManyWritesDeletes() throws Exception {
        final IntegrationTestKit testKit = new IntegrationTestKit(getSystem(), datastoreContextBuilder);
        CollectionNodeBuilder<MapEntryNode, MapNode> carMapBuilder = ImmutableNodes.mapNodeBuilder(CAR_QNAME);

        try (AbstractDataStore dataStore = testKit.setupAbstractDataStore(
                testParameter, "testManyWritesDeletes", "module-shards-cars-member-1.conf", true, "cars")) {

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            testKit.doCommit(writeTx.ready());

            Collection<Pair<YangInstanceIdentifier, MapEntryNode>> genCars = generateCars(20, "car");
            int carIndex = 0;
            for (Pair<YangInstanceIdentifier, MapEntryNode> car : genCars) {
                DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                YangInstanceIdentifier path = car.getKey();
                MapEntryNode data = car.getValue();

                rwTx.merge(path, data);
                carMapBuilder.withChild(data);

                testKit.doCommit(rwTx.ready());

                if (carIndex % 5 == 0) {
                    rwTx = txChain.newReadWriteTransaction();

                    rwTx.delete(path);
                    carMapBuilder.withoutChild(path.getLastPathArgument());
                    testKit.doCommit(rwTx.ready());
                }
                carIndex++;
            }

            final Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction()
                    .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());

            MapNode cars = carMapBuilder.build();

            assertEquals("cars not matching result", cars, optional.get());

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
        try (AbstractDataStore dataStore = testKit.setupAbstractDataStore(
                testParameter, "testManyWritesDeletes", "module-shards-cars-member-1.conf", true, "cars")) {

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();
            MapNode cars = carMapBuilder.build();

            final Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction()
                    .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());
            assertEquals("restored cars do not match snapshot", cars, optional.get());

            txChain.close();
        }
    }

    /**
     * Test writing cars with names 256KiB long (twice the size of maxEntrySize from the segmented-journal config).
     * Fragmentation will have to be used to persist these.
     */
    @Test
    public void testManyFragmentedWritesDeletes() throws Exception {
        final IntegrationTestKit testKit = new IntegrationTestKit(getSystem(), datastoreContextBuilder);
        CollectionNodeBuilder<MapEntryNode, MapNode> carMapBuilder = ImmutableNodes.mapNodeBuilder(CAR_QNAME);

        try (AbstractDataStore dataStore = testKit.setupAbstractDataStore(
            testParameter, "testManyWritesDeletes", "module-shards-cars-member-1.conf", true, "cars")) {

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            writeTx.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
            writeTx.write(CarsModel.CAR_LIST_PATH, CarsModel.newCarMapNode());
            testKit.doCommit(writeTx.ready());

            Collection<Pair<YangInstanceIdentifier, MapEntryNode>> genCars = generateCars(20,
                createStringOfSize(256 * 1024, 'a'));
            int carIndex = 0;
            for (Pair<YangInstanceIdentifier, MapEntryNode> car : genCars) {
                DOMStoreReadWriteTransaction rwTx = txChain.newReadWriteTransaction();

                YangInstanceIdentifier path = car.getKey();
                MapEntryNode data = car.getValue();

                rwTx.merge(path, data);
                carMapBuilder.withChild(data);

                testKit.doCommit(rwTx.ready());

                if (carIndex % 5 == 0) {
                    rwTx = txChain.newReadWriteTransaction();

                    rwTx.delete(path);
                    carMapBuilder.withoutChild(path.getLastPathArgument());
                    testKit.doCommit(rwTx.ready());
                }
                carIndex++;
            }

            final Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction()
                .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());

            MapNode cars = carMapBuilder.build();

            assertEquals("cars not matching result", cars, optional.get());

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
        try (AbstractDataStore dataStore = testKit.setupAbstractDataStore(
            testParameter, "testManyWritesDeletes", "module-shards-cars-member-1.conf", true, "cars")) {

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();
            MapNode cars = carMapBuilder.build();

            final Optional<NormalizedNode<?, ?>> optional = txChain.newReadOnlyTransaction()
                .read(CarsModel.CAR_LIST_PATH).get(5, TimeUnit.SECONDS);
            assertTrue("isPresent", optional.isPresent());
            assertEquals("restored cars do not match snapshot", cars, optional.get());

            txChain.close();
        }
    }

    private static String createStringOfSize(final int strSize, final char fillWithChar) {
        final StringBuilder sb = new StringBuilder(strSize);
        for (int i = 0; i < strSize; i++) {
            sb.append(fillWithChar);
        }
        return sb.toString();
    }
}
