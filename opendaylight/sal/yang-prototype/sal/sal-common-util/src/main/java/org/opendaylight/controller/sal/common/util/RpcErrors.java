/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.common.util;

import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;

/**
 * @author mirehak
 *
 */
public class RpcErrors {
    
    /**
     * @param applicationTag
     * @param tag
     * @param info
     * @param severity
     * @param message
     * @param errorType 
     * @param cause 
     * @return {@link RpcError} implementation
     */
    public static RpcError getRpcError(String applicationTag, String tag, String info,
            ErrorSeverity severity, String message, ErrorType errorType, Throwable cause) {
        RpcErrorTO ret = new RpcErrorTO(applicationTag, tag, info, severity, message, 
                errorType, cause);
        return ret;
    }

    private static class RpcErrorTO implements RpcError {

        private final String applicationTag;
        private final String tag;
        private final String info;
        private final ErrorSeverity severity;
        private final String message;
        private final ErrorType errorType;
        private final Throwable cause;

        /**
         * @param applicationTag
         * @param tag
         * @param info
         * @param severity
         * @param message
         * @param errorType
         * @param cause
         */
        protected RpcErrorTO(String applicationTag, String tag, String info,
                ErrorSeverity severity, String message, ErrorType errorType, Throwable cause) {
            super();
            this.applicationTag = applicationTag;
            this.tag = tag;
            this.info = info;
            this.severity = severity;
            this.message = message;
            this.errorType = errorType;
            this.cause = cause;
        }

        @Override
        public String getApplicationTag() {
            return applicationTag;
        }

        @Override
        public String getInfo() {
            return info;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public ErrorSeverity getSeverity() {
            return severity;
        }

        @Override
        public String getTag() {
            return tag;
        }

        @Override
        public Throwable getCause() {
            return cause;
        }
        
        @Override
        public ErrorType getErrorType() {
            return errorType;
        }
    }
}
