/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.benchmark.sharding.impl.DomListBuilder;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper.ShardData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides function for multi-threaded pushing of test data to data store.
 * @author jmedved
 *
 */
public class ShardTestCallable implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTestCallable.class);

    private final ShardData sd;
    private final int shardNum;
    private final long numItems;
    private final long opsPerTx;
    private final List<MapEntryNode> testData;

    private final SingleShardTest shardTest;

    int txSubmitted = 0;
    int txOk = 0;
    int txError = 0;

    ShardTestCallable(final SingleShardTest shardTest, final ShardData sd, int shardNum,
                      long numItems, long opsPerTx, final List<MapEntryNode> testData) {
        this.sd = sd;
        this.shardNum = shardNum;
        this.numItems = numItems;
        this.opsPerTx = opsPerTx;
        this.testData = testData;
        this.shardTest = shardTest;
    }

    ShardTestCallable(final ShardData sd, int shardNum, long numItems,
                      long opsPerTx, final List<MapEntryNode> testData) {
        this.sd = sd;
        this.shardNum = shardNum;
        this.numItems = numItems;
        this.opsPerTx = opsPerTx;
        this.testData = testData;
        this.shardTest = null;
        LOG.info("ShardTestCallable created");
    }

    @Override
    public Void call() throws Exception {
        for (int i = 0; i < numItems; i++) {
            NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                    DomListBuilder.IL_NAME, (long) i);
            MapEntryNode element;
            if (testData != null) {
                element = testData.get(i++);
            } else {
                element = AbstractShardTest.createListEntry(nodeId, shardNum, i);
            }

            shardTest.executeSingleWrite(element);
        }

        ShardTestOutput testOutput = shardTest.getTestResults();
        txOk += testOutput.getTxOk();
        txSubmitted += testOutput.getTxSubmitted();
        txError += testOutput.getTxError();

        return null;
    }

    public int getTxSubmitted() {
        return txSubmitted;
    }

    public int getTxOk() {
        return txOk;
    }

    public int getTxError() {
        return txError;
    }

}
