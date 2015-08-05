/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.mapping;

import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractLastNetconfOperation extends AbstractNetconfOperation {

    protected AbstractLastNetconfOperation(String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
    }

    @Override
    protected Element handle(Document document, XmlElement operationElement,
            NetconfOperationChainedExecution subsequentOperation) throws DocumentedException {
        if (!subsequentOperation.isExecutionTermination()){
            throw new DocumentedException(String.format("No netconf operation expected to be subsequent to %s, but is %s", this, subsequentOperation),
                    DocumentedException.ErrorType.application,
                    DocumentedException.ErrorTag.malformed_message,
                    DocumentedException.ErrorSeverity.error);
        }

        return handleWithNoSubsequentOperations(document, operationElement);
    }

    @Override
    protected HandlingPriority getHandlingPriority() {
        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY;
    }

    protected abstract Element handleWithNoSubsequentOperations(Document document, XmlElement operationElement) throws DocumentedException;
}
