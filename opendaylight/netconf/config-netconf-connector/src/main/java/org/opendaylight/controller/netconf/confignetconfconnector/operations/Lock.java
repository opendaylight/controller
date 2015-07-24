/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.facade.xml.Datastore;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple Lock implementation that pretends to lock candidate datastore.
 * Candidate datastore is allocated per session and is private so no real locking is needed (JMX is the only possible interference)
 */
public class Lock extends AbstractLastNetconfOperation {

    private static final Logger LOG = LoggerFactory.getLogger(Lock.class);

    private static final String LOCK = "lock";
    private static final String TARGET_KEY = "target";

    public Lock(final String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws DocumentedException {
        final Datastore targetDatastore = extractTargetParameter(operationElement);
        if(targetDatastore == Datastore.candidate) {
            // Since candidate datastore instances are allocated per session and not accessible anywhere else, no need to lock
            LOG.debug("Locking {} datastore on session: {}", targetDatastore, getNetconfSessionIdForReporting());
            // TODO should this fail if we are already locked ?
            return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
        }

        // Not supported running lock
        throw new DocumentedException("Unable to lock " + Datastore.running + " datastore", DocumentedException.ErrorType.application,
                DocumentedException.ErrorTag.operation_not_supported, DocumentedException.ErrorSeverity.error);
    }

    static Datastore extractTargetParameter(final XmlElement operationElement) throws DocumentedException {
        final XmlElement targetElement = operationElement.getOnlyChildElementWithSameNamespace(TARGET_KEY);
        final XmlElement targetChildNode = targetElement.getOnlyChildElementWithSameNamespace();

        return Datastore.valueOf(targetChildNode.getName());
    }

    @Override
    protected String getOperationName() {
        return LOCK;
    }
}
