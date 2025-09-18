/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark.simpletx;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.dsbenchmark.BaListBuilder;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpletxBaWrite extends DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxBaWrite.class);

    private final DataBroker dataBroker;
    private List<OuterList> list = null;

    public SimpletxBaWrite(final DataBroker dataBroker, final StartTestInput.Operation oper,
            final int outerListElem, final int innerListElem, final long writesPerTx, final DataStore dataStore) {
        super(oper, outerListElem, innerListElem, writesPerTx, dataStore);
        this.dataBroker = requireNonNull(dataBroker);
        LOG.debug("Created SimpletxBaWrite");
    }

    @Override
    public void createList() {
        list = BaListBuilder.buildOuterList(outerListElem, innerListElem);
    }

    @Override
    public void executeList() {
        final var dsType = getDataStoreType();

        var tx = dataBroker.newWriteOnlyTransaction();
        long writeCnt = 0;

        for (var element : list) {
            final var iid = DataObjectIdentifier.builder(TestExec.class).child(OuterList.class, element.key()).build();
            if (oper == StartTestInput.Operation.PUT) {
                tx.put(dsType, iid, element);
            } else {
                tx.merge(dsType, iid, element);
            }

            writeCnt++;

            if (writeCnt == writesPerTx) {
                try {
                    tx.commit().get();
                    txOk++;
                } catch (final InterruptedException | ExecutionException e) {
                    LOG.error("Transaction failed", e);
                    txError++;
                }
                tx = dataBroker.newWriteOnlyTransaction();

                writeCnt = 0;
            }
        }

        if (writeCnt != 0) {
            try {
                tx.commit().get();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Transaction failed", e);
            }
        }
    }
}
