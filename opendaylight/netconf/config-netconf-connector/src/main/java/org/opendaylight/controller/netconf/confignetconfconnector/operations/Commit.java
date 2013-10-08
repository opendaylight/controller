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

import org.opendaylight.controller.config.api.jmx.CommitStatus;
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

public class Commit extends AbstractConfigNetconfOperation {

    private static final Logger logger = LoggerFactory.getLogger(Commit.class);

    private final TransactionProvider transactionProvider;

    public Commit(TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.transactionProvider = transactionProvider;
    }

    private static void checkXml(XmlElement xml) {
        xml.checkName(XmlNetconfConstants.COMMIT);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    @Override
    protected String getOperationName() {
        return XmlNetconfConstants.COMMIT;
    }

    @Override
    protected Element handle(Document document, XmlElement xml) throws NetconfDocumentedException {
        checkXml(xml);

        CommitStatus status;
        try {
            status = this.transactionProvider.commitTransaction();
        } catch (final IllegalStateException e) {
            logger.warn("Commit failed: ", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put(ErrorTag.operation_failed.name(),
                    "Operation failed. Use 'get-config' or 'edit-config' before triggering 'commit' operation");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                    ErrorSeverity.error, errorInfo);
        } catch (final NetconfDocumentedException e) {
            throw new NetconfDocumentedException(
                    "Unable to retrieve config snapshot after commit for persister, details: " + e.getMessage(),
                    ErrorType.application, ErrorTag.operation_failed, ErrorSeverity.error, e.getErrorInfo());
        }
        logger.info("Datastore {} committed successfully: {}", Datastore.candidate, status);

        return document.createElement(XmlNetconfConstants.OK);
    }

}
