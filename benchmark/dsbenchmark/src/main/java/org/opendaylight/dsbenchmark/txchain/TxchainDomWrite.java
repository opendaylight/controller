/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark.txchain;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.dsbenchmark.DomListBuilder;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainDomWrite extends DatastoreAbstractWriter implements FutureCallback<Empty> {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainDomWrite.class);

    private final DOMDataBroker dataBroker;
    private List<MapEntryNode> list = null;

    public TxchainDomWrite(final DOMDataBroker dataBroker, final StartTestInput.Operation oper,
            final int outerListElem, final int innerListElem, final long writesPerTx, final DataStore dataStore) {
        super(oper, outerListElem, innerListElem, writesPerTx, dataStore);
        this.dataBroker = dataBroker;
        LOG.debug("Created TxchainDomWrite");
    }

    @Override
    public void createList() {
        list = DomListBuilder.buildOuterList(outerListElem, innerListElem);
    }

    @Override
    public void executeList() {
        final var dsType = getDataStoreType();
        final var pid = YangInstanceIdentifier.of(TestExec.QNAME, OuterList.QNAME);
        final var chain = dataBroker.createMergingTransactionChain();
        chain.addCallback(this);

        var tx = chain.newWriteOnlyTransaction();
        int txSubmitted = 0;
        int writeCnt = 0;

        for (var element : list) {
            var yid = pid.node(NodeIdentifierWithPredicates.of(OuterList.QNAME, element.name().asMap()));

            if (oper == StartTestInput.Operation.PUT) {
                tx.put(dsType, yid, element);
            } else {
                tx.merge(dsType, yid, element);
            }

            writeCnt++;

            // Start performing the operation; submit the transaction at every n-th operation
            if (writeCnt == writesPerTx) {
                txSubmitted++;
                tx.commit().addCallback(new FutureCallback<CommitInfo>() {
                    @Override
                    public void onSuccess(final CommitInfo result) {
                        txOk++;
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        LOG.error("Transaction failed", cause);
                        txError++;
                    }
                }, MoreExecutors.directExecutor());
                tx = chain.newWriteOnlyTransaction();
                writeCnt = 0;
            }
        }

        // *** Clean up and close the transaction chain ***
        // Submit the outstanding transaction even if it's empty and wait for it to finish
        // We need to empty the transaction chain before closing it

        try {
            txSubmitted++;
            tx.commit().get();
            txOk++;
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Transaction failed", e);
            txError++;
        }
        try {
            chain.close();
        } catch (final IllegalStateException e) {
            LOG.error("Transaction close failed,", e);
        }

        LOG.debug("Transactions: submitted {}, completed {}", txSubmitted, txOk + txError);
    }

    @Override
    public void onFailure(final Throwable cause) {
        LOG.error("Broken chain in TxchainDomWrite", cause);
    }

    @Override
    public void onSuccess(final Empty result) {
        LOG.debug("Chain closed successfully");
    }
}
