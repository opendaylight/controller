/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.benchmark.sharding.impl.DomListBuilder;
import org.opendaylight.controller.benchmark.sharding.impl.ShardFactory;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper;
import org.opendaylight.controller.benchmark.sharding.impl.tests.ShardTestStats.TestStatus;
import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Performs test where there each shard has its won thread that pushes data
 * into the shard.
 * @author jmedved
 *
 */
public class MultiThreadShardTest extends AbstractShardTest {
    private static final Logger LOG = LoggerFactory.getLogger(MultiThreadShardTest.class);

    MultiThreadShardTest(final long numShards, final long numItems, final long numListeners, final long opsPerTx,
            final LogicalDatastoreType dataStoreType, final boolean precreateTestData, final ShardHelper shardHelper,
            final DOMDataTreeService dataTreeService, final ShardFactory shardFactory) throws ShardTestException {

        super(numShards, numItems, numListeners, opsPerTx, dataStoreType, precreateTestData, shardHelper,
                dataTreeService, shardFactory);
        LOG.info("Created MultiThreadShardTest");
    }

    /** Pre-creates per-shard test data (InnerList elements) before the
     *  measured test run and puts them in an array list for quick retrieval
     *  during the test run.
     * @return: the list of pre-created test elements that will be pushed
     *          into the data store during the test run.
     */
    private List<MapEntryNode> preCreatePerShardTestData(final int shardNum) {
        final List<MapEntryNode> testData;
        if (preCreateTestData) {
            LOG.info("Pre-creating test data for Shard {}...", shardNum);
            testData = Lists.newArrayList();
            for (long i = 0; i < numItems; i++) {
                NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, i);
                testData.add(createListEntry(nodeId, shardNum, i));
            }
            LOG.info("   Done. {} elements created.", testData.size());
        } else {
            LOG.info("No test data pre-created for Shard {}", shardNum);
            testData = null;
        }
        return testData;
    }

    @Override
    public ShardTestStats runTest() {
        LOG.info("Running MultiThreadShardTest");

        final List<ShardTestCallable> callables = Lists.newArrayList();
        int shard = 0;
        try {
            for (SingleShardTest shardTest : createTestShardLayout()) {
                callables.add(new ShardTestCallable(shardTest, shardData.get(shard),
                        shard, numItems, opsPerTx, preCreatePerShardTestData(shard)));
            }
        } catch (ShardTestException | DOMDataTreeShardCreationFailedException
                | DOMDataTreeProducerException | DOMDataTreeShardingConflictException e) {
            LOG.error("Shard test failed", e);
        }

        final ExecutorService executorService = Executors.newFixedThreadPool((int) numShards);
        boolean allThreadsCompleted = true;
        final long startTime = System.nanoTime();
        try {
            final List<Future<Void>> futures = executorService.invokeAll(callables, 10, TimeUnit.MINUTES);

            for (int i = 0; i < futures.size(); i++) {
                Future<Void> future = futures.get(i);
                if (future.isCancelled()) {
                    allThreadsCompleted = false;
                    LOG.info("{}. thread timed out.", i + 1);
                } else {
                    try {
                        future.get();
                    } catch (final ExecutionException e) {
                        allThreadsCompleted = false;
                        LOG.info("{}. thread failed.", i + 1, e);
                    }
                }
            }
        } catch (final InterruptedException e) {
            allThreadsCompleted = false;
            LOG.warn("Unable to execute requests", e);
        } finally {
            executorService.shutdownNow();
        }
        final long endTime = System.nanoTime();

        LOG.info("MultiThreadShardTest finished");
        // Get statistics for each writer thread
        int txOk = 0;
        int txError = 0;
        int txSubmitted = 0;
        for (ShardTestCallable c : callables) {
            txOk += c.getTxOk();
            txError += c.getTxError();
            txSubmitted += c.getTxSubmitted();
        }
        return new ShardTestStats(allThreadsCompleted == true ? TestStatus.OK : TestStatus.ERROR,
                txOk, txError, txSubmitted,
                (endTime - startTime) / 1000, getListenerEventsOk(), getListenerEventsFail());
    }
}
