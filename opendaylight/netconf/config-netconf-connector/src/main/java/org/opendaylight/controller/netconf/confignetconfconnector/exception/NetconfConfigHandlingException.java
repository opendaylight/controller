/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.confignetconfconnector.exception;

import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;

public class NetconfConfigHandlingException extends NetconfDocumentedException {
    private static final long serialVersionUID = 1L;

    public NetconfConfigHandlingException(final String message, final ErrorType errorType, final ErrorTag errorTag,
                                          final ErrorSeverity errorSeverity) {
        this(message, errorType, errorTag, errorSeverity, Collections.<String, String>emptyMap());
    }

    public NetconfConfigHandlingException(final String message, final ErrorType errorType, final ErrorTag errorTag,
                                          final ErrorSeverity errorSeverity, final Map<String, String> errorInfo){
        super(message,errorType,errorTag,errorSeverity,errorInfo);
    }
}
