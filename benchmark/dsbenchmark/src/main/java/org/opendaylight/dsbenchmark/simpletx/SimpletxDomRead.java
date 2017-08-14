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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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


public class SimpletxDomRead extends DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxDomRead.class);
    private final DOMDataBroker domDataBroker;

    public SimpletxDomRead(final DOMDataBroker domDataBroker, final int outerListElem,
                           final int innerListElem, final long writesPerTx, final DataStore dataStore) {
        super(StartTestInput.Operation.DELETE, outerListElem, innerListElem, writesPerTx, dataStore);
        this.domDataBroker = domDataBroker;
        LOG.debug("Created simpleTxDomRead");

    }

    @Override
    public void createList() {
        LOG.debug("SimpletxDomRead: creating data in the data store");
        // Dump the whole list into the data store in a single transaction
        // with <outerListElem> PUTs on the transaction
        SimpletxDomWrite dd = new SimpletxDomWrite(domDataBroker,
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
        final org.opendaylight.yangtools.yang.common.QName olId = QName.create(OuterList.QNAME, "id");
        final YangInstanceIdentifier pid =
                YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();

        try (DOMDataReadOnlyTransaction tx = domDataBroker.newReadOnlyTransaction()) {
            for (int l = 0; l < outerListElem; l++) {
                YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, olId, l));
                CheckedFuture<Optional<NormalizedNode<?,?>>, ReadFailedException> submitFuture = tx.read(dsType, yid);
                try {
                    Optional<NormalizedNode<?,?>> optionalDataObject = submitFuture.checkedGet();
                    if (optionalDataObject != null && optionalDataObject.isPresent()) {
                        NormalizedNode<?, ?> ret = optionalDataObject.get();
                        LOG.trace("optionalDataObject is {}", ret);
                        txOk++;
                    } else {
                        txError++;
                        LOG.warn("optionalDataObject is either null or .isPresent is false");
                    }
                } catch (final ReadFailedException e) {
                    LOG.warn("failed to ....", e);
                    txError++;
                }
            }
        }
    }
}
