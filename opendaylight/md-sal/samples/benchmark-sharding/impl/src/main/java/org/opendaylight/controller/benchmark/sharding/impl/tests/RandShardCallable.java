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
import java.util.concurrent.Callable;
import org.opendaylight.controller.benchmark.sharding.impl.DomListBuilder;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper.ShardData;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides function for randomly pushing of test data to data store with multi-thread.
 * @author kucnh
 *
 */
public class RandShardCallable implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(RandShardCallable.class);

    private final ShardData sd;
    private final int shardNum;
    private final long opsPerTx;
    private final List<MapEntryNode> testData;

    int txSubmitted = 0;
    int txOk = 0;
    int txError = 0;
    int itemIndex = 0;
    int writeCnt = 0;

    public RandShardCallable(ShardData sd, int shardNum, long opsPerTx, List<MapEntryNode> testData) {
        this.sd = sd;
        this.shardNum = shardNum;
        this.opsPerTx = opsPerTx;
        this.testData = testData;

        LOG.info("RandShardCallable created");
    }

    @Override
    public Void call() throws Exception {
        DOMDataTreeCursorAwareTransaction tx = sd.getProducer().createTransaction(false);
        DOMDataTreeWriteCursor cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
        if (cursor != null) {
            cursor.enter(new YangInstanceIdentifier.NodeIdentifier(InnerList.QNAME));
        } else {
            LOG.error("The cursor is NULL");
        }

        YangInstanceIdentifier.NodeIdentifierWithPredicates nodeId =
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, (long)itemIndex);
        MapEntryNode element;

        if (testData != null) {
            element = testData.get(itemIndex);
        } else {
            element = AbstractShardTest.createListEntry(nodeId, shardNum, itemIndex);
        }

        writeCnt++;
        if (cursor != null) {
            cursor.write(nodeId, element);
        } else {
            LOG.error("The cursor is NULL");
        }

        if (writeCnt == opsPerTx) {
            // We have reached the limit of writes-per-transaction.
            // Submit the current outstanding transaction and create
            // a new one in its place.
            txSubmitted++;
            if (cursor != null) {
                cursor.close();
            } else {
                LOG.error("The cursor is NULL");
            }

            Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    txOk++;
                }

                @Override
                public void onFailure(final Throwable t1) {
                    LOG.error("Transaction failed, shard {}, exception {}", shardNum, t1);
                    txError++;
                }
            });

            writeCnt = 0;
            tx = sd.getProducer().createTransaction(false);
            cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
            if (cursor != null) {
                cursor.enter(new YangInstanceIdentifier.NodeIdentifier(InnerList.QNAME));
            } else {
                LOG.error("The cursor is NULL");
            }
        }
        if (cursor != null) {
            cursor.close();
        } else {
            LOG.error("The cursor is NULL");
        }

        itemIndex ++;

        // Submit the last outstanding transaction even if it's empty and wait
        // for it to complete. This will flush all outstanding transactions to
        // the data store. Note that all tx submits except for the last one are
        // asynchronous.
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Last transaction submit failed, shard {}, exception {}", shardNum, e);
            txError++;
        }
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

    public int getItemIndex() {
        return itemIndex;
    }

    public int getShardNum() {
        return shardNum;
    }
}
