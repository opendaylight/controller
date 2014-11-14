/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.mapping.operations;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorSeverity;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorTag;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException.ErrorType;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
public class DefaultStartExi extends AbstractSingletonNetconfOperation implements DefaultNetconfOperation {
    public static final String START_EXI = "start-exi";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultStartExi.class);
    private NetconfServerSession netconfSession;

    public DefaultStartExi(String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    public Document handle(Document message,
                           NetconfOperationChainedExecution subsequentOperation) throws NetconfDocumentedException {
        LOG.debug("Received start-exi message {} ", XmlUtil.toString(message));

        try {
            netconfSession.startExiCommunication(new NetconfMessage(message));
        } catch (IllegalArgumentException e) {
            throw new NetconfDocumentedException("Failed to parse EXI parameters", ErrorType.protocol,
                    ErrorTag.operation_failed, ErrorSeverity.error);
        }

        return super.handle(message, subsequentOperation);
    }

    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws NetconfDocumentedException {
        Element getSchemaResult = document.createElementNS( XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, XmlNetconfConstants.OK);
        LOG.trace("{} operation successful", START_EXI);
        return getSchemaResult;
    }

    @Override
    protected String getOperationName() {
        return START_EXI;
    }

    @Override
    protected String getOperationNamespace() {
        return XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0;
    }

    @Override
    public void setNetconfSession(NetconfServerSession s) {
        netconfSession = s;
    }
}
