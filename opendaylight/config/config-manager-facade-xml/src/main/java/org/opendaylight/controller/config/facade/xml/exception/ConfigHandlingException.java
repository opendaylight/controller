/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.facade.xml.exception;

import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.config.util.xml.DocumentedException;

public class ConfigHandlingException extends DocumentedException {
    private static final long serialVersionUID = 1L;

    public ConfigHandlingException(final String message, final ErrorType errorType, final ErrorTag errorTag,
            final ErrorSeverity errorSeverity) {
        this(message, null, errorType, errorTag, errorSeverity, Collections.<String, String>emptyMap());
    }

    public ConfigHandlingException(final String message, final ErrorType errorType, final ErrorTag errorTag,
            final ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        this(message, null, errorType, errorTag, errorSeverity, errorInfo);
    }

    public ConfigHandlingException(final String message, final Throwable cause, final ErrorType errorType,
            final ErrorTag errorTag, final ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        super(message, cause, errorType, errorTag, errorSeverity, errorInfo);
    }
}
