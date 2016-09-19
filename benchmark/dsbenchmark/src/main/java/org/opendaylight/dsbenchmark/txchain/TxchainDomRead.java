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

import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxchainDomRead extends DatastoreAbstractWriter implements TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainDomRead.class);
    private final DOMDataBroker domDataBroker;

    public TxchainDomRead(DOMDataBroker domDataBroker, int outerListElem, int innerListElem,
            long writesPerTx, DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.info("Created TxchainDomDelete");
    }

    @Override
    public void createList() {
        LOG.info("TxchainDomDelete: creating data in the data store");

        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        TxchainDomWrite dd = new TxchainDomWrite(domDataBroker,
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

        org.opendaylight.yangtools.yang.common.QName olId = QName.create(OuterList.QNAME, "id");
        DOMDataReadOnlyTransaction tx = domDataBroker.newReadOnlyTransaction();

        YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        for (int l = 0; l < outerListElem; l++) {
            YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, olId, l));
            Optional<NormalizedNode<?,?>> optionalDataObject;
            CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> submitFuture =
                    tx.read(LogicalDatastoreType.CONFIGURATION, yid);
            try {
                optionalDataObject = submitFuture.checkedGet();
                if (optionalDataObject != null && optionalDataObject.isPresent()) {
                    txOk++;
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
        LOG.error("Broken chain {} in TxchainDomDelete, transaction {}, cause {}",
                chain, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.info("TxchainDomDelete closed successfully, chain {}", chain);
    }
}
