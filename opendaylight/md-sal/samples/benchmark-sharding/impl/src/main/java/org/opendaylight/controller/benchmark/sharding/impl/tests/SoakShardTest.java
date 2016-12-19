/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.benchmark.sharding.impl.DomListBuilder;
import org.opendaylight.controller.benchmark.sharding.impl.ShardFactory;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper.ShardData;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoakShardTest extends AbstractShardTest {
    private static final Logger LOG = LoggerFactory.getLogger(SoakShardTest.class);
    private final long operations;

    private final AtomicInteger txOk = new AtomicInteger();
    private final AtomicInteger txError = new AtomicInteger();

    SoakShardTest(final long numShards, final long numItems, final long operations, final long numListeners,
        final long opsPerTx, final LogicalDatastoreType dataStoreType, final boolean precreateTestData,
        final ShardHelper shardHelper, final DOMDataTreeService dataTreeService, final ShardFactory shardFactory)
                throws ShardTestException {
        super(numShards, numItems, numListeners, opsPerTx, dataStoreType, precreateTestData,
                shardHelper, dataTreeService, shardFactory);
        this.operations = operations;
        LOG.info("Created SoakShardTest");
    }

    @Override
    public ShardTestStats runTest() {
        LOG.info("Running SoakShardTest");

        final List<MapEntryNode> testData = preCreateTestData();

        DOMDataTreeCursorAwareTransaction[] tx = new DOMDataTreeCursorAwareTransaction[(int) numShards];
        DOMDataTreeWriteCursor[] cursor = new DOMDataTreeWriteCursor[(int) numShards];
        int[] writeCnt = new int[(int) numShards];

        for (int s = 0; s < numShards; s++) {
            writeCnt[s] = 0;
            ShardData sd = shardData.get(s);
            tx[s] = sd.getProducer().createTransaction(false);
            cursor[s] = tx[s].createCursor(sd.getDOMDataTreeIdentifier());
            cursor[s].enter(new NodeIdentifier(InnerList.QNAME));
        }

        int txSubmitted = 0;
        // Random rnd = new Random();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int dataSetSize = (int)(numItems * numShards);
        final long startTime = System.nanoTime();

        for (int op = 0; op < operations; op++) {
            int testDataIdx = rnd.nextInt(dataSetSize);
            int shardIdx = testDataIdx % (int)numShards;
            long dataIdx = testDataIdx / numShards;
            // LOG.info("op {}: testDataIdx {}, shardIdx {}, dataIdx {}", op, testDataIdx, shardIdx, dataIdx);

            NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                    DomListBuilder.IL_NAME, dataIdx);
            MapEntryNode element;
            if (preCreateTestData) {
                element = testData.get(testDataIdx);
            } else {
                element = createListEntry(nodeId, shardIdx, dataIdx);
            }
            writeCnt[shardIdx]++;
            cursor[shardIdx].write(nodeId, element);

            if (writeCnt[shardIdx] == opsPerTx) {
                // We have reached the limit of writes-per-transaction.
                // Submit the current outstanding transaction and create
                // a new one in its place.
                txSubmitted++;
                cursor[shardIdx].close();
                Futures.addCallback(tx[shardIdx].submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        txOk.incrementAndGet();
                    }

                    @Override
                    public void onFailure(final Throwable t1) {
                        LOG.error("Transaction failed, {}", t1);
                        txError.incrementAndGet();
                    }
                });

                writeCnt[shardIdx] = 0;
                ShardData sd = shardData.get(shardIdx);
                tx[shardIdx] = sd.getProducer().createTransaction(false);
                cursor[shardIdx] = tx[shardIdx].createCursor(sd.getDOMDataTreeIdentifier());
                cursor[shardIdx].enter(new NodeIdentifier(InnerList.QNAME));
            }
        }

        // Submit the last outstanding transaction even if it's empty and wait
        // for it to complete. This will flush all outstanding transactions to
        // the data store. Note that all tx submits except for the last one are
        // asynchronous.
        for (int s = 0; s < numShards; s++) {
            txSubmitted++;
            cursor[s].close();
            try {
                tx[s].submit().checkedGet();
                txOk.incrementAndGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed, {}", e);
                txError.incrementAndGet();
            }
        }

        final long endTime = System.nanoTime();
        LOG.info("SoakShardTest finished");
        return new ShardTestStats(ShardTestStats.TestStatus.OK, txOk.intValue(), txError.intValue(), txSubmitted,
                (endTime - startTime) / 1000, getListenerEventsOk(), getListenerEventsFail());
    }
}
