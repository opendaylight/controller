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
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainBaRead extends DatastoreAbstractWriter implements TransactionChainListener{
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(TxchainBaRead.class);
    private DataBroker bindingDataBroker;

    public TxchainBaRead(DataBroker bindingDataBroker, int outerListElem, int innerListElem, long writesPerTx) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx);
        this.bindingDataBroker = bindingDataBroker;
        LOG.info("Created TxchainBaRead");
    }

    @Override
    public void createList() {
        LOG.info("TxchainBaRead: reading data in the data store");

        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        TxchainBaWrite dd = new TxchainBaWrite(bindingDataBroker,
                StartTestInput.Operation.PUT,
                outerListElem,
                innerListElem,
                outerListElem);
        dd.createList();
        dd.executeList();
    }

    @Override
    public void executeList() {

        BindingTransactionChain chain = bindingDataBroker.createTransactionChain(this);
        ReadTransaction tx = bindingDataBroker.newReadOnlyTransaction();

        for (long l = 0; l < outerListElem; l++) {

            OuterList outerList;
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                    .child(OuterList.class, new OuterListKey((int) l));
            Optional<OuterList> optionalDataObject;
            CheckedFuture<Optional<OuterList>, ReadFailedException> submitFuture = tx.read(LogicalDatastoreType.CONFIGURATION, iid);

            try {
                optionalDataObject = submitFuture.checkedGet();
                /*if (optionalDataObject != null && optionalDataObject.isPresent()) {
                    ret = optionalDataObject.get();
                    txOk++;
                }*/
                if (optionalDataObject != null && optionalDataObject.isPresent()) {
                    outerList = optionalDataObject.get();

                    String[] objectsArray = new String[outerList.getInnerList().size()];

                    //LOG.info("innerList element: " + objectsArray );
                    for (InnerList innerList : outerList.getInnerList()) {
                        if (objectsArray[innerList.getName()] != null) {
                            LOG.error("innerList: DUPLICATE name: {}, value: {}", innerList.getName(), innerList.getValue());
                        }
                        objectsArray[innerList.getName()] = innerList.getValue();
                        // LOG.info("innerList: name: {}, value: {}", innerList.getName(), innerList.getValue());
                    }
                    boolean foundAll = true;
                    for (int i = 0; i < outerList.getInnerList().size(); i++) {
                        String itemStr = objectsArray[i];
                        if (!itemStr.contentEquals("Item-" + String.valueOf(l) + "-" + String.valueOf(i))) {
                            foundAll = false;
                            LOG.error("innerList: name: {}, value: {}", i, itemStr);
                            break;
                        }
                    }
                    txOk++;
                }
                else {
                    txError++;
                }
            } catch (ReadFailedException e) {
                LOG.warn("failed to ....", e);
                txError++;
            }
        }
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
                                         AsyncTransaction<?, ?> transaction, Throwable cause) {
        LOG.error("Broken chain {} in TxchainBaDelete, transaction {}, cause {}",
                chain, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.info("TxchainBaDelete closed successfully, chain {}", chain);
    }

}
