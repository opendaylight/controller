/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util.xml;

import java.util.Collections;
import java.util.Map;

public class UnexpectedElementException extends DocumentedException {
    private static final long serialVersionUID = 1L;

    public UnexpectedElementException(final String message, final DocumentedException.ErrorType errorType, final DocumentedException.ErrorTag errorTag,
                                      final DocumentedException.ErrorSeverity errorSeverity) {
        this(message, errorType, errorTag, errorSeverity, Collections.<String, String> emptyMap());
    }

    public UnexpectedElementException(final String message, final DocumentedException.ErrorType errorType, final DocumentedException.ErrorTag errorTag,
                                      final DocumentedException.ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        super(message,errorType,errorTag,errorSeverity,errorInfo);
    }
}
