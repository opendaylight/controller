/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.simpletx;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpletxDomDelete extends DatastoreAbstractWriter {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(SimpletxDomDelete.class);
    private final DOMDataBroker domDataBroker;

    public SimpletxDomDelete(DOMDataBroker domDataBroker, int outerListElem,
            int innerListElem, long writesPerTx) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx);
        this.domDataBroker = domDataBroker;
        LOG.info("Created simpleTxDomDelete");
   }

    @Override
    public void createList() {
        LOG.info("SimpletxDomDelete: creating data in the data store");
        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        SimpletxDomWrite dd = new SimpletxDomWrite(domDataBroker,
                                                   StartTestInput.Operation.PUT,
                                                   outerListElem,
                                                   innerListElem,
                                                   outerListElem);
        dd.createList();
        dd.executeList();
    }

    @Override
    public void executeList() {
        long writeCnt = 0;

        org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(OuterList.QNAME, "id");
        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();

        for (int l = 0; l < outerListElem; l++) {
            YangInstanceIdentifier yid = YangInstanceIdentifier.builder()
                                         .node(TestExec.QNAME)
                                         .node(OuterList.QNAME)
                                         .nodeWithKey(OuterList.QNAME, OL_ID, l)
                                         .build();

            tx.delete(LogicalDatastoreType.CONFIGURATION, yid);
            writeCnt++;
            if (writeCnt == writesPerTx) {
                try {
                    tx.submit().checkedGet();
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed: {}", e.toString());
                    txError++;
                }
                tx = domDataBroker.newWriteOnlyTransaction();
                writeCnt = 0;
            }
        }
        if (writeCnt != 0) {
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed: {}", e.toString());
            }
        }
    }

}
