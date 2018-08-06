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
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainDomWrite extends DatastoreAbstractWriter implements DOMTransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainDomWrite.class);
    private final DOMDataBroker domDataBroker;
    private List<MapEntryNode> list;

    public TxchainDomWrite(final DOMDataBroker domDataBroker, final StartTestInput.Operation oper,
            final int outerListElem, final int innerListElem, final long writesPerTx, final DataStore dataStore) {
        super(oper, outerListElem, innerListElem, writesPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.debug("Created TxchainDomWrite");
    }

    @Override
    public void createList() {
        list = DomListBuilder.buildOuterList(this.outerListElem, this.innerListElem);
    }

    @Override
    public void executeList() {
        final LogicalDatastoreType dsType = getDataStoreType();
        final YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        final DOMTransactionChain chain = domDataBroker.createTransactionChain(this);

        DOMDataTreeWriteTransaction tx = chain.newWriteOnlyTransaction();
        int txSubmitted = 0;
        int writeCnt = 0;

        for (MapEntryNode element : this.list) {
            YangInstanceIdentifier yid =
                    pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, element.getIdentifier().getKeyValues()));

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
    public void onTransactionChainFailed(final DOMTransactionChain chain, final DOMDataTreeTransaction transaction,
            final Throwable cause) {
        LOG.error("Broken chain {} in TxchainDomWrite, transaction {}, cause {}", chain, transaction.getIdentifier(),
            cause);
    }

    @Override
    public void onTransactionChainSuccessful(final DOMTransactionChain chain) {
        LOG.debug("Chain {} closed successfully", chain);
    }
}
