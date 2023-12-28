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
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.dsbenchmark.DomListBuilder;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
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

    private final DOMDataBroker dataBroker;
    private List<MapEntryNode> list = null;

    public SimpletxDomWrite(final DOMDataBroker dataBroker, final StartTestInput.Operation oper,
            final int outerListElem, final int innerListElem, final long putsPerTx, final DataStore dataStore) {
        super(oper, outerListElem, innerListElem, putsPerTx, dataStore);
        this.dataBroker = requireNonNull(dataBroker);
        LOG.debug("Created SimpletxDomWrite");
    }

    @Override
    public void createList() {
        list = DomListBuilder.buildOuterList(outerListElem, innerListElem);
    }

    @Override
    public void executeList() {
        final var dsType = getDataStoreType();
        final var pid = YangInstanceIdentifier.of(TestExec.QNAME, OuterList.QNAME);

        var tx = dataBroker.newWriteOnlyTransaction();
        long writeCnt = 0;

        for (var element : list) {
            final var yid = pid.node(NodeIdentifierWithPredicates.of(OuterList.QNAME, element.name().asMap()));

            if (oper == StartTestInput.Operation.PUT) {
                tx.put(dsType, yid, element);
            } else {
                tx.merge(dsType, yid, element);
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
