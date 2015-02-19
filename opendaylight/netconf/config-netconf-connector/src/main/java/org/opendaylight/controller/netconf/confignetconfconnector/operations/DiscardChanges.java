/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class DiscardChanges extends AbstractConfigNetconfOperation {

    public static final String DISCARD = "discard-changes";

    private static final Logger LOG = LoggerFactory.getLogger(DiscardChanges.class);

    private final TransactionProvider transactionProvider;

    public DiscardChanges(final TransactionProvider transactionProvider, ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(configRegistryClient, netconfSessionIdForReporting);
        this.transactionProvider = transactionProvider;
    }

    private static void fromXml(XmlElement xml) throws NetconfDocumentedException {
        xml.checkName(DISCARD);
        xml.checkNamespace(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    @Override
    protected String getOperationName() {
        return DISCARD;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement xml) throws NetconfDocumentedException {
        fromXml(xml);
        try {
            if (transactionProvider.getTransaction().isPresent()) {
                this.transactionProvider.abortTransaction();
            }
        } catch (final RuntimeException e) {
            LOG.warn("Abort failed: ", e);
            final Map<String, String> errorInfo = new HashMap<>();
            errorInfo
                    .put(ErrorTag.operation_failed.name(),
                            "Abort failed.");
            throw new NetconfDocumentedException(e.getMessage(), e, ErrorType.application, ErrorTag.operation_failed,
                    ErrorSeverity.error, errorInfo);
        }
        LOG.trace("Changes discarded successfully from datastore {}", Datastore.candidate);


        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }
}
