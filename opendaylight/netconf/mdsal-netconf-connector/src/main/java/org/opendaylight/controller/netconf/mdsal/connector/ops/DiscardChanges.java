/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mdsal.connector.TransactionProvider;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DiscardChanges extends AbstractLastNetconfOperation{

    private static final Logger LOG = LoggerFactory.getLogger(DiscardChanges.class);

    private static final String OPERATION_NAME = "discard-changes";

    private final TransactionProvider transactionProvider;

    public DiscardChanges(final String netconfSessionIdForReporting, final TransactionProvider transactionProvider) {
        super(netconfSessionIdForReporting);
        this.transactionProvider = transactionProvider;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        operationElement.getOnlyChildElement(OPERATION_NAME);

        try {
            transactionProvider.abortTransaction();
        } catch (IllegalStateException e) {
            LOG.warn("Abort failed ", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo
                    .put(ErrorTag.operation_failed.name(),
                            "Operation failed. Use 'get-config' or 'edit-config' before triggering 'discard-changes' operation");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                    ErrorSeverity.error, errorInfo);
        }
        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }
}
