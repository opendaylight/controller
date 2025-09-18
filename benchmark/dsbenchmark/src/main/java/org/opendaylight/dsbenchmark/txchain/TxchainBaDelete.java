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
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainBaDelete extends DatastoreAbstractWriter implements FutureCallback<Empty> {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainBaDelete.class);
    private final DataBroker bindingDataBroker;

    public TxchainBaDelete(final DataBroker bindingDataBroker, final int outerListElem, final int innerListElem,
            final long writesPerTx, final DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.bindingDataBroker = bindingDataBroker;
        LOG.debug("Created TxchainBaDelete");
    }

    @Override
    public void createList() {
        LOG.debug("TxchainBaDelete: creating data in the data store");

        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        TxchainBaWrite dd = new TxchainBaWrite(bindingDataBroker, StartTestInput.Operation.PUT, outerListElem,
            innerListElem, outerListElem, dataStore);
        dd.createList();
        dd.executeList();
    }

    @Override
    public void executeList() {
        final LogicalDatastoreType dsType = getDataStoreType();
        final TransactionChain chain = bindingDataBroker.createMergingTransactionChain();
        chain.addCallback(this);

        WriteTransaction tx = chain.newWriteOnlyTransaction();
        int txSubmitted = 0;
        int writeCnt = 0;

        for (int l = 0; l < outerListElem; l++) {
           tx.delete(dsType,
                DataObjectIdentifier.builder(TestExec.class).child(OuterList.class, new OuterListKey(l)).build());

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

        // Submit the outstanding transaction even if it's empty and wait for it to finish
        // We need to empty the chain before closing it
        try {
            if (writeCnt > 0) {
                txSubmitted++;
            }
            tx.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Transaction failed", e);
        }
        try {
            chain.close();
        } catch (final IllegalStateException e) {
            LOG.error("Transaction close failed", e);
        }
        LOG.debug("Transactions: submitted {}, completed {}", txSubmitted, txOk + txError);
    }

    @Override
    public void onFailure(final Throwable cause) {
        LOG.error("Broken chain in TxchainBaDelete", cause);
    }

    @Override
    public void onSuccess(final Empty chain) {
        LOG.debug("TxchainBaDelete closed successfully");
    }
}
