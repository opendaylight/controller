/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops.get;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
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

    private static final Logger LOG = LoggerFactory.getLogger(GetConfig.class);

    private static final String OPERATION_NAME = "get";

    private final TransactionProvider transactionProvider;

    public Get(final String netconfSessionIdForReporting, final CurrentSchemaContext schemaContext, final TransactionProvider transactionProvider) {
        super(netconfSessionIdForReporting, schemaContext);
        this.transactionProvider = transactionProvider;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws NetconfDocumentedException {
        try {
            final GetConfigExecution getConfigExecution = GetConfigExecution.fromXml(operationElement, OPERATION_NAME);

            // TODO how to read candidate
            Preconditions.checkArgument(getConfigExecution.getDatastore() == Datastore.running);
        } catch (final NetconfDocumentedException e) {
            LOG.warn("Get request processing failed on session: {}", getNetconfSessionIdForReporting(), e);
            throw e;
        }

        final YangInstanceIdentifier dataRoot = ROOT;
        DOMDataReadWriteTransaction rwTx = transactionProvider.getOrCreateTransaction();
        try {
            final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = rwTx.read(LogicalDatastoreType.OPERATIONAL, dataRoot).checkedGet();
            final Element dataElement = XmlUtil.createElement(document, XmlNetconfConstants.DATA_KEY, Optional.<String>absent());
            if (normalizedNodeOptional.isPresent()) {
                dataElement.appendChild(transformNormalizedNode(normalizedNodeOptional.get(), dataRoot));
            }
            return dataElement;
        } catch (ReadFailedException e) {
            LOG.warn("Unable to read data: {}", dataRoot, e);
            throw new IllegalStateException("Unable to read data " + dataRoot, e);
        }

    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }
}
