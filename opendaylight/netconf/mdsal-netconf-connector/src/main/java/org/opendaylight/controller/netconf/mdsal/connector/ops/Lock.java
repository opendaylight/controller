/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedNamespaceException;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Lock extends AbstractLastNetconfOperation{

    private static final Logger LOG = LoggerFactory.getLogger(Lock.class);

    private static final String OPERATION_NAME = "lock";
    private static final String TARGET_KEY = "target";

    public Lock(final String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        final Datastore targetDatastore = extractTargetParameter(operationElement);
        if (targetDatastore == Datastore.candidate) {
            LOG.debug("Locking candidate datastore on session: {}", getNetconfSessionIdForReporting());
            return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
        }

        throw new NetconfDocumentedException("Unable to lock " + targetDatastore + " datastore", NetconfDocumentedException.ErrorType.application,
                NetconfDocumentedException.ErrorTag.operation_not_supported, NetconfDocumentedException.ErrorSeverity.error);
    }

    static Datastore extractTargetParameter(final XmlElement operationElement) throws NetconfDocumentedException {
        final XmlElement targetChildNode;
        try {
            final XmlElement targetElement = operationElement.getOnlyChildElementWithSameNamespace(TARGET_KEY);
            targetChildNode = targetElement.getOnlyChildElementWithSameNamespace();
        } catch (final MissingNameSpaceException | UnexpectedNamespaceException e) {
            LOG.trace("Can't get only child element with same namespace", e);
            throw NetconfDocumentedException.wrap(e);
        }

        return Datastore.valueOf(targetChildNode.getName());
    }

    @Override
    protected String getOperationName() {
        return OPERATION_NAME;
    }
}
