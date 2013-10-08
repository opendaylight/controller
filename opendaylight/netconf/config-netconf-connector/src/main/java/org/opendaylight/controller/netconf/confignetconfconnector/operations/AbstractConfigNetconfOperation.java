/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractConfigNetconfOperation extends AbstractNetconfOperation {

    protected final ConfigRegistryClient configRegistryClient;

    protected AbstractConfigNetconfOperation(ConfigRegistryClient configRegistryClient,
            String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
        this.configRegistryClient = configRegistryClient;
    }

    @Override
    protected HandlingPriority canHandle(String operationName, String operationNamespace) {
        // TODO check namespace
        return operationName.equals(getOperationName()) ? HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY
                : HandlingPriority.CANNOT_HANDLE;
    }

    protected abstract String getOperationName();

    @Override
    protected Element handle(Document document, XmlElement operationElement, NetconfOperationRouter opRouter)
            throws NetconfDocumentedException {
        return handle(document, operationElement);
    }

    protected abstract Element handle(Document document, XmlElement operationElement) throws NetconfDocumentedException;
}
