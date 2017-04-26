/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.txchain;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainBaRead extends DatastoreAbstractWriter implements TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainBaRead.class);
    private final DataBroker bindingDataBroker;

    public TxchainBaRead(final DataBroker bindingDataBroker, final int outerListElem, final int innerListElem,
            final long writesPerTx, final DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.bindingDataBroker = bindingDataBroker;
        LOG.debug("Created TxchainBaRead");
    }

    @Override
    public void createList() {
        LOG.debug("TxchainBaRead: reading data in the data store");

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

        try (ReadOnlyTransaction tx = bindingDataBroker.newReadOnlyTransaction()) {
            for (long l = 0; l < outerListElem; l++) {

                InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                        .child(OuterList.class, new OuterListKey((int) l));
                CheckedFuture<Optional<OuterList>, ReadFailedException> submitFuture =
                        tx.read(dsType, iid);

                try {
                    Optional<OuterList> optionalDataObject = submitFuture.checkedGet();
                    if (optionalDataObject != null && optionalDataObject.isPresent()) {
                        OuterList outerList = optionalDataObject.get();

                        String[] objectsArray = new String[outerList.getInnerList().size()];
                        for (InnerList innerList : outerList.getInnerList()) {
                            if (objectsArray[innerList.getName()] != null) {
                                LOG.error("innerList: DUPLICATE name: {}, value: {}", innerList.getName(),
                                    innerList.getValue());
                            }
                            objectsArray[innerList.getName()] = innerList.getValue();
                        }
                        for (int i = 0; i < outerList.getInnerList().size(); i++) {
                            String itemStr = objectsArray[i];
                            if (!itemStr.contentEquals("Item-" + String.valueOf(l) + "-" + String.valueOf(i))) {
                                LOG.error("innerList: name: {}, value: {}", i, itemStr);
                                break;
                            }
                        }
                        txOk++;
                    } else {
                        txError++;
                    }
                } catch (ReadFailedException e) {
                    LOG.warn("failed to ....", e);
                    txError++;
                }
            }
        }
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
