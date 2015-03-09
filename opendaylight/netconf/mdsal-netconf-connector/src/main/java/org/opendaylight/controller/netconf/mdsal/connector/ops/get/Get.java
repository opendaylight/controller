/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops.get;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.mdsal.connector.TransactionProvider;
import org.opendaylight.controller.netconf.mdsal.connector.ops.Datastore;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Get extends AbstractGet {

    private static final Logger LOG = LoggerFactory.getLogger(Get.class);

    private static final String OPERATION_NAME = "get";
    private final TransactionProvider transactionProvider;

    public Get(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext, final TransactionProvider transactionProvider) {
        super(netconfSessionIdForReporting, schemaContext);
        this.transactionProvider = transactionProvider;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws NetconfDocumentedException {

        final YangInstanceIdentifier dataRoot;

        Optional<XmlElement> filterElement = operationElement.getOnlyChildElementOptionally(FILTER);
        if (filterElement.isPresent()) {
            if (filterElement.get().getChildElements().size() == 0) {
                //empty filter response should be empty data container
                return XmlUtil.createElement(document, XmlNetconfConstants.DATA_KEY, Optional.<String>absent());
            }
            dataRoot = getInstanceIdentifierFromFilter(filterElement.get());
        } else {
            dataRoot = ROOT;
        }

        DOMDataReadWriteTransaction rwTx = getTransaction(Datastore.running);
        try {
            final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = rwTx.read(LogicalDatastoreType.OPERATIONAL, dataRoot).checkedGet();
            transactionProvider.abortRunningTransaction(rwTx);

            if (!normalizedNodeOptional.isPresent()) {
                return XmlUtil.createElement(document, XmlNetconfConstants.DATA_KEY, Optional.<String>absent());
            }

            return serializeNodeWithParentStructure(document, dataRoot, normalizedNodeOptional.get());
        } catch (ReadFailedException e) {
            LOG.warn("Unable to read data: {}", dataRoot, e);
            throw new IllegalStateException("Unable to read data " + dataRoot, e);
        }
    }

    private DOMDataReadWriteTransaction getTransaction(Datastore datastore) throws NetconfDocumentedException{
        if (datastore == Datastore.candidate) {
            return transactionProvider.getOrCreateTransaction();
        } else if (datastore == Datastore.running) {
            return transactionProvider.createRunningTransaction();
        }
        throw new NetconfDocumentedException("Incorrect Datastore: ", ErrorType.protocol, ErrorTag.bad_element, ErrorSeverity.error);
    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }
}
