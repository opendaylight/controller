/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api;

import java.util.Collections;
import java.util.Map;

import org.opendaylight.protocol.framework.DocumentedException;

/**
 * Checked exception to communicate an error that needs to be sent to the
 * netconf client.
 */
public class NetconfDocumentedException extends DocumentedException {

    private static final long serialVersionUID = 1L;

    public enum ErrorType {
        transport, rpc, protocol, application;

        public String getTagValue() {
            return name();
        }
    }

    public enum ErrorTag {
        missing_attribute("missing-attribute"), unknown_element("unknown-element"), operation_not_supported(
                "operation-not-supported"), bad_attribute("bad-attribute"), data_missing("data-missing"), operation_failed(
                "operation-failed"), invalid_value("invalid-value"), malformed_message("malformed-message");

        private final String tagValue;

        ErrorTag(final String tagValue) {
            this.tagValue = tagValue;
        }

        public String getTagValue() {
            return this.tagValue;
        }
    }

    public enum ErrorSeverity {
        error, warning;

        public String getTagValue() {
            return name();
        }
    }

    private final ErrorType errorType;
    private final ErrorTag errorTag;
    private final ErrorSeverity errorSeverity;
    private final Map<String, String> errorInfo;

    public NetconfDocumentedException(final String message, final ErrorType errorType, final ErrorTag errorTag,
            final ErrorSeverity errorSeverity) {
        this(message, errorType, errorTag, errorSeverity, Collections.<String, String> emptyMap());
    }

    public NetconfDocumentedException(final String message, final ErrorType errorType, final ErrorTag errorTag,
            final ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        super(message);
        this.errorType = errorType;
        this.errorTag = errorTag;
        this.errorSeverity = errorSeverity;
        this.errorInfo = errorInfo;
    }

    public NetconfDocumentedException(final String message, final Exception cause, final ErrorType errorType,
            final ErrorTag errorTag, final ErrorSeverity errorSeverity) {
        this(message, cause, errorType, errorTag, errorSeverity, Collections.<String, String> emptyMap());
    }

    public NetconfDocumentedException(final String message, final Exception cause, final ErrorType errorType,
            final ErrorTag errorTag, final ErrorSeverity errorSeverity, final Map<String, String> errorInfo) {
        super(message, cause);
        this.errorType = errorType;
        this.errorTag = errorTag;
        this.errorSeverity = errorSeverity;
        this.errorInfo = errorInfo;
    }

    public ErrorType getErrorType() {
        return this.errorType;
    }

    public ErrorTag getErrorTag() {
        return this.errorTag;
    }

    public ErrorSeverity getErrorSeverity() {
        return this.errorSeverity;
    }

    public Map<String, String> getErrorInfo() {
        return this.errorInfo;
    }

    @Override
    public String toString() {
        return "NetconfDocumentedException{" + "message=" + getMessage() + ", errorType=" + this.errorType
                + ", errorTag=" + this.errorTag + ", errorSeverity=" + this.errorSeverity + ", errorInfo="
                + this.errorInfo + '}';
    }
}
