/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark.simpletx;

import java.util.concurrent.ExecutionException;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpletxBaDelete extends DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxBaDelete.class);
    private final DataBroker dataBroker;

    public SimpletxBaDelete(final DataBroker dataBroker, final int outerListElem, final int innerListElem,
            final long writesPerTx, final DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.dataBroker = dataBroker;
        LOG.debug("Created SimpletxBaDelete");
    }

    @Override
    public void createList() {
        LOG.debug("DatastoreDelete: creating data in the data store");
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
        final LogicalDatastoreType dsType = getDataStoreType();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        long putCnt = 0;

        for (long l = 0; l < outerListElem; l++) {
            tx.delete(dsType, DataObjectIdentifier.builder(TestExec.class)
                .child(OuterList.class, new OuterListKey((int) l))
                .build());
            putCnt++;
            if (putCnt == writesPerTx) {
                try {
                    tx.commit().get();
                    txOk++;
                } catch (final InterruptedException | ExecutionException e) {
                    LOG.error("Transaction failed", e);
                    txError++;
                }
                tx = dataBroker.newWriteOnlyTransaction();
                putCnt = 0;
            }
        }
        if (putCnt != 0) {
            try {
                tx.commit().get();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Transaction failed", e);
            }
        }
    }
}
