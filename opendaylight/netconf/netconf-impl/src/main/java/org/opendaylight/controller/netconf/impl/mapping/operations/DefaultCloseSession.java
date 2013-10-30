/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.mapping.api.DefaultNetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCloseSession extends AbstractNetconfOperation implements DefaultNetconfOperation {
    public static final String CLOSE_SESSION = "close-session";
    private NetconfSession netconfSession;

    public DefaultCloseSession(String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected HandlingPriority canHandle(String operationName, String netconfOperationNamespace) {
        if (operationName.equals(CLOSE_SESSION) == false)
            return HandlingPriority.CANNOT_HANDLE;
        if (netconfOperationNamespace.equals(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0) == false)
            return HandlingPriority.CANNOT_HANDLE;

        return HandlingPriority.HANDLE_WITH_MAX_PRIORITY;
    }

    /**
     * Close netconf operation router associated to this session, which in turn
     * closes NetconfOperationServiceSnapshot with all NetconfOperationService
     * instances
     */
    @Override
    protected Element handle(Document document, XmlElement operationElement, NetconfOperationRouter opRouter)
            throws NetconfDocumentedException {
        opRouter.close();
        return document.createElement(XmlNetconfConstants.OK);
    }

    @Override
    public void setNetconfSession(NetconfSession s) {
        this.netconfSession = s;
    }

    public NetconfSession getNetconfSession() {
        return netconfSession;
    }
}
