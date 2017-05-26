/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.txchain;

import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class TxchainDomDelete extends DatastoreAbstractWriter implements TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainBaWrite.class);
    private final DOMDataBroker domDataBroker;

    public TxchainDomDelete(DOMDataBroker domDataBroker, int outerListElem, int innerListElem,
            long writesPerTx, DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.info("Created TxchainDomDelete");
    }

    @Override
    public void createList() {
        LOG.info("TxchainDomDelete: creating data in the data store");

        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        TxchainDomWrite dd = new TxchainDomWrite(domDataBroker,
                                                 StartTestInput.Operation.PUT,
                                                 outerListElem,
                                                 innerListElem,
                                                 outerListElem,
                                                 dataStore);
        dd.createList();
        dd.executeList();
    }

    @Override
    public void executeList() {
        int txSubmitted = 0;
        int writeCnt = 0;

        org.opendaylight.yangtools.yang.common.QName olId = QName.create(OuterList.QNAME, "id");
        DOMTransactionChain chain = domDataBroker.createTransactionChain(this);
        DOMDataWriteTransaction tx = chain.newWriteOnlyTransaction();

        YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        for (int l = 0; l < outerListElem; l++) {
            YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, olId, l));
            tx.delete(LogicalDatastoreType.CONFIGURATION, yid);

            writeCnt++;

            if (writeCnt == writesPerTx) {
                txSubmitted++;
                Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        txOk++;
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        LOG.error("Transaction failed, {}", t);
                        txError++;
                    }
                });
                tx = chain.newWriteOnlyTransaction();
                writeCnt = 0;
            }
        }

        // *** Clean up and close the transaction chain ***
        // Submit the outstanding transaction even if it's empty and wait for it to finish
        // We need to empty the transaction chain before closing it
        try {
            txSubmitted++;
            tx.submit().checkedGet();
            txOk++;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed", e);
            txError++;
        }
        try {
            chain.close();
        } catch (IllegalStateException e) {
            LOG.error("Transaction close failed,", e);
        }
        LOG.info("Transactions: submitted {}, completed {}", txSubmitted, (txOk + txError));
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
            AsyncTransaction<?, ?> transaction, Throwable cause) {
        LOG.error("Broken chain {} in TxchainDomDelete, transaction {}, cause {}",
                chain, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.info("TxchainDomDelete closed successfully, chain {}", chain);
    }
}
