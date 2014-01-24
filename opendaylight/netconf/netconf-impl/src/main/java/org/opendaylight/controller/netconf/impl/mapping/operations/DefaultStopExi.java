/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.mapping.operations;

import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.mapping.api.DefaultNetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultStopExi extends AbstractNetconfOperation implements DefaultNetconfOperation {

    public static final String STOP_EXI = "stop-exi";
    private NetconfSession netconfSession;

    private static final Logger logger = LoggerFactory
            .getLogger(DefaultStartExi.class);

    public DefaultStopExi(String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected HandlingPriority canHandle(String operationName,
            String netconfOperationNamespace) {
        if (operationName.equals(STOP_EXI) == false)
            return HandlingPriority.CANNOT_HANDLE;
        if (netconfOperationNamespace
                .equals(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0) == false)
            return HandlingPriority.CANNOT_HANDLE;

        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
    }

    @Override
    protected Element handle(Document document, XmlElement operationElement,
            NetconfOperationRouter opRouter) throws NetconfDocumentedException {
        throw new UnsupportedOperationException("Not implemented");
        /*
        netconfSession.remove(ExiDecoderHandler.class);
        netconfSession.removeAfterMessageSent(ExiEncoderHandler.HANDLER_NAME);

        Element getSchemaResult = document.createElement(XmlNetconfConstants.OK);
        XmlUtil.addNamespaceAttr(getSchemaResult,
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
        logger.trace("{} operation successful", STOP_EXI);
        logger.debug("received stop-exi message {} ", XmlUtil.toString(document));
        return getSchemaResult;
        */
    }

    @Override
    public void setNetconfSession(NetconfSession s) {
        this.netconfSession = s;
    }

    public NetconfSession getNetconfSession() {
        return netconfSession;
    }
}
