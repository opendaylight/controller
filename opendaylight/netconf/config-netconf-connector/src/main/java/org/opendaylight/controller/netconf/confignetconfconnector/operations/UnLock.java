/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple unlock implementation that pretends to unlock candidate datastore.
 * Candidate datastore is allocated per session and is private so no real locking is needed (JMX is the only possible interference)
 */
public class UnLock extends AbstractLastNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(UnLock.class);

    private static final String UNLOCK = "unlock";

    public UnLock(final String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        final Datastore targetDatastore = Lock.extractTargetParameter(operationElement);
        if(targetDatastore == Datastore.candidate) {
            // Since candidate datastore instances are allocated per session and not accessible anywhere else, no need to lock
            LOG.debug("Unlocking {} datastore on session: {}", targetDatastore, getNetconfSessionIdForReporting());
            // TODO this should fail if we are not locked
            return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
        }

        // Not supported running lock
        throw new NetconfDocumentedException("Unable to unlock " + Datastore.running + " datastore", NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.operation_not_supported, NetconfDocumentedException.ErrorSeverity.error);
    }

    @Override
    protected String getOperationName() {
        return UNLOCK;
    }
}
