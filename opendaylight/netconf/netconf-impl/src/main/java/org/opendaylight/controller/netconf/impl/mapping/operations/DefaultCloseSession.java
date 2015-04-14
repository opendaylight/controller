/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collections;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.NetconfServerSession;
import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCloseSession extends AbstractSingletonNetconfOperation implements DefaultNetconfOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCloseSession.class);

    public static final String CLOSE_SESSION = "close-session";

    private final AutoCloseable sessionResources;
    private NetconfServerSession session;

    public DefaultCloseSession(String netconfSessionIdForReporting, AutoCloseable sessionResources) {
        super(netconfSessionIdForReporting);
        this.sessionResources = sessionResources;
    }

    @Override
    protected String getOperationName() {
        return CLOSE_SESSION;
    }

    /**
     * Close netconf operation router associated to this session, which in turn
     * closes NetconfOperationServiceSnapshot with all NetconfOperationService
     * instances
     */
    @Override
    protected Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement)
            throws NetconfDocumentedException {
        try {
            sessionResources.close();
            Preconditions.checkNotNull(session, "Session was not set").delayedClose();
            LOG.info("Session {} closing", session.getSessionId());
        } catch (Exception e) {
            throw new NetconfDocumentedException("Unable to properly close session "
                    + getNetconfSessionIdForReporting(), NetconfDocumentedException.ErrorType.APPLICATION,
                    NetconfDocumentedException.ErrorTag.OPERATION_FAILED,
                    NetconfDocumentedException.ErrorSeverity.ERROR, Collections.singletonMap(
                        NetconfDocumentedException.ErrorSeverity.ERROR.toString(), e.getMessage()));
        }
        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    @Override
    public void setNetconfSession(final NetconfServerSession s) {
        this.session = s;
    }
}
