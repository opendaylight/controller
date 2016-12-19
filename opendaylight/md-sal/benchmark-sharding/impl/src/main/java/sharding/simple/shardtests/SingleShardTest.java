/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleShardTest {
    private static Logger LOG = LoggerFactory.getLogger(SingleShardTest.class);

    private final DOMDataTreeProducer producer;
    private final DOMDataTreeIdentifier anchorId;
    private final long opsPerTx;

    private DOMDataTreeCursorAwareTransaction wTx;
    private DOMDataTreeWriteCursor cursor;
    private long opCount;

    private long txSubmitted = 0;
    private final AtomicLong txOk = new AtomicLong();
    private final AtomicLong txError = new AtomicLong();
    private final DOMDataTreeIdentifier prefix;


    public SingleShardTest(final DOMDataTreeService dataTreeService, final DOMDataTreeIdentifier prefix,
                           final long maxOperationsPerTx) {
        this.producer = dataTreeService.createProducer(Collections.singleton(prefix));
        this.prefix = prefix;
        this.opsPerTx = maxOperationsPerTx;
        this.anchorId =
                new DOMDataTreeIdentifier(prefix.getDatastoreType(), prefix.getRootIdentifier().node(InnerList.QNAME));
    }

    void executeSingleWrite(MapEntryNode entryNode) {
        cursor.write(entryNode.getIdentifier(), entryNode);
        opCount++;

        if(opCount == opsPerTx) {
            cursor.close();
            Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    txOk.incrementAndGet();
                }

                @Override
                public void onFailure(Throwable t) {
                    LOG.error("Transaction {} failed. {}", wTx.getIdentifier(), t);
                    txOk.incrementAndGet();
                }
            });
            txSubmitted++;
            initCursor();
        }
    }

    ShardTestOutput getTestResults() {
        cursor.close();
        txSubmitted++;
        try {
            wTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            txError.getAndIncrement();
        }

        return new ShardTestOutputBuilder().setTxError(txError.get())
                .setTxOk(txOk.get())
                .setTxSubmitted(txSubmitted)
                .build();
    }

    void initCursor() {
        opCount = 0;
        wTx = producer.createTransaction(false);
        cursor = wTx.createCursor(anchorId);
    }

    DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    DOMDataTreeProducer getProducer() {
        return producer;
    }
}
