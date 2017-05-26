/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.simpletx;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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


public class SimpletxBaRead extends DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxBaRead.class);
    private DataBroker dataBroker;

    public SimpletxBaRead(DataBroker dataBroker, int outerListElem, int innerListElem,
            long writesPerTx, DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.dataBroker = dataBroker;
        LOG.info("Created SimpletxBaRead");
    }

    @Override
    public void createList() {
        LOG.info("DatastoreRead: creating data in the data store");
        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        SimpletxBaWrite dd = new SimpletxBaWrite(dataBroker,
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
        ReadTransaction tx = dataBroker.newReadOnlyTransaction();

        for (long l = 0; l < outerListElem; l++) {

            OuterList outerList;
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                    .child(OuterList.class, new OuterListKey((int)l));
            Optional<OuterList> optionalDataObject;
            CheckedFuture<Optional<OuterList>, ReadFailedException> submitFuture =
                    tx.read(LogicalDatastoreType.CONFIGURATION, iid);
            try {
                optionalDataObject = submitFuture.checkedGet();
                if (optionalDataObject != null && optionalDataObject.isPresent()) {
                    outerList = optionalDataObject.get();

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
