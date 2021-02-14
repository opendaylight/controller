/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dsbenchmark.txchain;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
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

public class TxchainDomRead extends DatastoreAbstractWriter implements DOMTransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(TxchainDomRead.class);
    private final DOMDataBroker domDataBroker;

    public TxchainDomRead(final DOMDataBroker domDataBroker, final int outerListElem, final int innerListElem,
            final long writesPerTx, final DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.debug("Created TxchainDomDelete");
    }

    @Override
    public void createList() {
        LOG.debug("TxchainDomDelete: creating data in the data store");

        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        TxchainDomWrite dd = new TxchainDomWrite(domDataBroker, StartTestInput.Operation.PUT, outerListElem,
            innerListElem, outerListElem, dataStore);
        dd.createList();
        dd.executeList();
    }

    @Override
    public void executeList() {
        final LogicalDatastoreType dsType = getDataStoreType();
        final org.opendaylight.yangtools.yang.common.QName olId = QName.create(OuterList.QNAME, "id");
        final YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();

        try (DOMDataTreeReadTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            for (int l = 0; l < outerListElem; l++) {
                YangInstanceIdentifier yid = pid.node(NodeIdentifierWithPredicates.of(OuterList.QNAME, olId, l));
                Optional<NormalizedNode> optionalDataObject;
                FluentFuture<Optional<NormalizedNode>> submitFuture = tx.read(dsType, yid);
                try {
                    optionalDataObject = submitFuture.get();
                    if (optionalDataObject != null && optionalDataObject.isPresent()) {
                        txOk++;
                    }
                } catch (final InterruptedException | ExecutionException e) {
                    LOG.warn("failed to ....", e);
                    txError++;
                }
            }
        }
    }

    @Override
    public void onTransactionChainFailed(final DOMTransactionChain chain, final DOMDataTreeTransaction transaction,
            final Throwable cause) {
        LOG.error("Broken chain {} in TxchainDomDelete, transaction {}", chain, transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(final DOMTransactionChain chain) {
        LOG.debug("TxchainDomDelete closed successfully, chain {}", chain);
    }
}
