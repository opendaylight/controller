/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DiscardChanges extends AbstractConfigNetconfOperation {

    public static final String DISCARD = "discard-changes";

    private static final Logger logger = LoggerFactory.getLogger(DiscardChanges.class);

    private final TransactionProvider transactionProvider;

    public DiscardChanges(final TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.transactionProvider = transactionProvider;
    }

    private static void fromXml(XmlElement xml) {
        xml.checkName(DISCARD);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    @Override
    protected String getOperationName() {
        return DISCARD;
    }

    @Override
    protected Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {
        try {
            fromXml(xml);
        } catch (final IllegalArgumentException e) {
            logger.warn("Rpc error: {}", ErrorTag.bad_attribute, e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.bad_attribute.name(), e.getMessage());
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.rpc, ErrorTag.bad_attribute,
                    ErrorSeverity.error, errorInfo);
        }

        try {
            this.transactionProvider.abortTransaction();
        } catch (final IllegalStateException e) {
            logger.warn("Abort failed: ", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo
                    .put(ErrorTag.operation_failed.name(),
                            "Operation failed. Use 'get-config' or 'edit-config' before triggering 'discard-changes' operation");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                    ErrorSeverity.error, errorInfo);
        }
        logger.info("Changes discarded successfully from datastore {}", Datastore.candidate);

        return document.createElement(XmlNetconfConstants.OK);
    }
}
