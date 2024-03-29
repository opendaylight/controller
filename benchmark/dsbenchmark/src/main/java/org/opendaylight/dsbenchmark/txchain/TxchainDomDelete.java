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
import java.util.concurrent.ExecutionException;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainDomDelete extends DatastoreAbstractWriter implements FutureCallback<Empty> {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainDomDelete.class);
    private final DOMDataBroker domDataBroker;

    public TxchainDomDelete(final DOMDataBroker domDataBroker, final int outerListElem, final int innerListElem,
            final long writesPerTx, final DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.debug("Created TxchainDomDelete");
    }

    @Override
    public void createList() {
        LOG.debug("TxchainDomDelete: creating data in the data store");

        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        TxchainDomWrite dd = new TxchainDomWrite(domDataBroker, StartTestInput.Operation.PUT, outerListElem,
            innerListElem, outerListElem, dataStore);
        dd.createList();
        dd.executeList();
    }

    @Override
    public void executeList() {
        final LogicalDatastoreType dsType = getDataStoreType();
        final org.opendaylight.yangtools.yang.common.QName olId = QName.create(OuterList.QNAME, "id");
        final YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        final DOMTransactionChain chain = domDataBroker.createMergingTransactionChain();
        chain.addCallback(this);

        DOMDataTreeWriteTransaction tx = chain.newWriteOnlyTransaction();
        int txSubmitted = 0;
        int writeCnt = 0;

        for (int l = 0; l < outerListElem; l++) {
            YangInstanceIdentifier yid = pid.node(NodeIdentifierWithPredicates.of(OuterList.QNAME, olId, l));
            tx.delete(dsType, yid);

            writeCnt++;

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
        LOG.error("Broken chain in TxchainDomDelete", cause);
    }

    @Override
    public void onSuccess(final Empty result) {
        LOG.debug("TxchainDomDelete closed successfully");
    }
}
