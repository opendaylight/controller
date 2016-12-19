/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sharding.simple.impl.DomListBuilder;
import sharding.simple.impl.ShardFactory;
import sharding.simple.impl.ShardHelper;
import sharding.simple.shardtests.ShardTestStats.TestStatus;

/** Performs test where there each shard has its won thread that pushes data
 * randomly into the shard.
 * @author kunch
 *
 */
public class RandomShardTest extends AbstractShardTest {

    private static final Logger LOG = LoggerFactory.getLogger(RandomShardTest.class);

    RandomShardTest(Long numShards, Long numItems, Long numListeners, Long opsPerTx,
                    LogicalDatastoreType dataStoreType, Boolean precreateTestData, ShardHelper shardHelper,
                    DOMDataTreeService dataTreeService, ShardFactory shardFactory) throws ShardTestException {

        super(numShards, numItems, numListeners, opsPerTx, dataStoreType, precreateTestData, shardHelper,
                dataTreeService, shardFactory);
        LOG.info("Created RandomShardTest");
    }

    /** Pre-creates per-shard test data (InnerList elements) before the
     *  measured test run and puts them in an array list for quick retrieval
     *  during the test run.
     *  @return the list of pre-created test elements that will be pushed
     *          into the data store during the test run.
     */
    protected List<MapEntryNode> preCreateTestData() {
        final List<MapEntryNode> testData;
        if (preCreateTestData) {
            LOG.info("Pre-creating test data for all Shards...");
            testData = Lists.newArrayList();
            for (int i = 0; i < numItems * numShards; i++) {
                NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, (long)i);
                testData.add(createListEntry(nodeId, 1, i));
            }
            LOG.info("   Done. {} elements created.", testData.size());
        } else {
            LOG.info("No test data pre-created for Shard");
            testData = null;
        }
        return testData;
    }

    @Override
    public ShardTestStats runTest() {
        LOG.info("Running RandomShardTest");

        int rand;
        int txOK = 0;
        int txError = 0;
        int txSubmitted = 0;
        final ExecutorService executorService = Executors.newFixedThreadPool((int)numShards);
        boolean allThreadsCompleted = true;
        final long startTime = System.nanoTime();

        final List<RandShardCallable> callables = Lists.newArrayList();
        final List<MapEntryNode> testDatas = preCreateTestData();
        for (int i = 0; i < numShards; i++) {
            callables.add(i, new RandShardCallable(shardData.get(i), i, opsPerTx, testDatas));
        }

        for (int i = 0; i < numShards * numItems; i++) {
            rand = ThreadLocalRandom.current().nextInt(0, (int)numShards);
            try {
                Future<Void> future = executorService.submit(callables.get(rand));
                if (future.isCancelled()) {
                    allThreadsCompleted = false;
                    LOG.warn("Feature cancelled, {} thread timed out.", rand);
                } else {
                    try {
                        future.get();
                    } catch (final ExecutionException e) {
                        allThreadsCompleted = false;
                        LOG.warn("{} thread timed out.", rand, e);
                    }
                }
            } catch (final InterruptedException e) {
                allThreadsCompleted = false;
                LOG.warn("Unable to execute requests", e);
            }
        }

        executorService.shutdown();
        final long endTime = System.nanoTime();

        LOG.info("RandomShardTest finished");
        for (RandShardCallable c : callables) {
            txOK += c.getTxOk();
            txError += c.getTxError();
            txSubmitted += c.getTxSubmitted();
            LOG.info("{} shard handled {} items", c.getShardNum(), c.getItemIndex());
        }

        return new ShardTestStats(allThreadsCompleted ? TestStatus.OK : TestStatus.ERROR,
                txOK, txError, txSubmitted, (endTime - startTime) / 1000,
                getListenerEventsOk(), getListenerEventsFail());

    }
}
