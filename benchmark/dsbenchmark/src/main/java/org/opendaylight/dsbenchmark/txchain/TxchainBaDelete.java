/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.txchain;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainBaDelete extends DatastoreAbstractWriter implements TransactionChainListener {
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
        TxchainBaWrite dd = new TxchainBaWrite(bindingDataBroker,
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
        final LogicalDatastoreType dsType = getDataStoreType();
        final BindingTransactionChain chain = bindingDataBroker.createTransactionChain(this);

        WriteTransaction tx = chain.newWriteOnlyTransaction();
        int txSubmitted = 0;
        int writeCnt = 0;

        for (int l = 0; l < outerListElem; l++) {
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                                                    .child(OuterList.class, new OuterListKey(l));
            tx.delete(dsType, iid);

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

        // Submit the outstanding transaction even if it's empty and wait for it to finish
        // We need to empty the chain before closing it
        try {
            if (writeCnt > 0) {
                txSubmitted++;
            }
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed", e);
        }
        try {
            chain.close();
        } catch (IllegalStateException e) {
            LOG.error("Transaction close failed,", e);
        }
        LOG.debug("Transactions: submitted {}, completed {}", txSubmitted, (txOk + txError));
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
            final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Broken chain {} in TxchainBaDelete, transaction {}, cause {}",
                chain, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("TxchainBaDelete closed successfully, chain {}", chain);
    }

}
