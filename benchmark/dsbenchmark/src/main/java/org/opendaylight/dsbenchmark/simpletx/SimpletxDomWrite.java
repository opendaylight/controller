/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark.simpletx;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.dsbenchmark.DomListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpletxDomWrite extends DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxDomWrite.class);
    private final DOMDataBroker domDataBroker;
    private List<MapEntryNode> list;

    public SimpletxDomWrite(DOMDataBroker domDataBroker, StartTestInput.Operation oper,
                                    int outerListElem, int innerListElem, long putsPerTx, DataStore dataStore ) {
        super(oper, outerListElem, innerListElem, putsPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.info("Created SimpletxDomWrite");
    }

    @Override
    public void createList() {
        list = DomListBuilder.buildOuterList(this.outerListElem, this.innerListElem);
    }

    @Override
    public void executeList() {
        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        LogicalDatastoreType dsType = getDataStoreType();
        long writeCnt = 0;

        YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        for (MapEntryNode element : this.list) {
            YangInstanceIdentifier yid =
                    pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, element.getIdentifier().getKeyValues()));

            if (oper == StartTestInput.Operation.PUT) {
                tx.put(dsType, yid, element);
            } else {
                tx.merge(dsType, yid, element);
            }

            writeCnt++;

            if (writeCnt == writesPerTx) {
                try {
                    tx.submit().checkedGet();
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed", e);
                    txError++;
                }
                tx = domDataBroker.newWriteOnlyTransaction();
                dsType = getDataStoreType();
                writeCnt = 0;
            }
        }

        if (writeCnt != 0) {
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed", e);
            }
        }

    }

}
