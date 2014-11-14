/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import com.google.common.base.Optional;
import java.util.Collections;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.mapping.AbstractSingletonNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DefaultCloseSession extends AbstractSingletonNetconfOperation {
    public static final String CLOSE_SESSION = "close-session";
    private final AutoCloseable sessionResources;

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
        } catch (Exception e) {
            throw new NetconfDocumentedException("Unable to properly close session "
                    + getNetconfSessionIdForReporting(), NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error, Collections.singletonMap(
                        NetconfDocumentedException.ErrorSeverity.error.toString(), e.getMessage()));
        }
        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }
}
