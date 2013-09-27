/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This exception is not intended to be used while implementing modules,
 * itaggregates validation exceptions and sends them back to the user.
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = -6072893219820274247L;

    private final Map<String, Map<String, ExceptionMessageWithStackTrace>> failedValidations;

    public ValidationException(
            Map<String /* module name */, Map<String /* instance name */, ExceptionMessageWithStackTrace>> failedValidations) {
        super(failedValidations.toString());
        this.failedValidations = Collections.unmodifiableMap(failedValidations);
    }

    public static ValidationException createFromCollectedValidationExceptions(
            List<ValidationException> collectedExceptions) {
        Map<String, Map<String, ExceptionMessageWithStackTrace>> failedValidations = new HashMap<>();
        for (ValidationException ve : collectedExceptions) {
            for (Entry<String, Map<String, ExceptionMessageWithStackTrace>> outerEntry : ve
                    .getFailedValidations().entrySet()) {
                for (Entry<String, ExceptionMessageWithStackTrace> innerEntry : outerEntry
                        .getValue().entrySet()) {
                    String moduleName = outerEntry.getKey();
                    String instanceName = innerEntry.getKey();
                    ExceptionMessageWithStackTrace ex = innerEntry.getValue();
                    Map<String, ExceptionMessageWithStackTrace> instanceToExMap = failedValidations
                            .get(moduleName);
                    if (instanceToExMap == null) {
                        instanceToExMap = new HashMap<>();
                        failedValidations.put(moduleName, instanceToExMap);
                    }
                    if (instanceToExMap.containsKey(instanceName)) {
                        throw new IllegalArgumentException(
                                "Cannot merge with same module name "
                                        + moduleName + " and instance name "
                                        + instanceName);
                    }
                    instanceToExMap.put(instanceName, ex);
                }
            }
        }
        return new ValidationException(failedValidations);
    }

    public static ValidationException createForSingleException(
            ModuleIdentifier moduleIdentifier, Exception e) {
        Map<String, Map<String, ExceptionMessageWithStackTrace>> failedValidations = new HashMap<>();
        Map<String, ExceptionMessageWithStackTrace> innerMap = new HashMap<>();

        failedValidations.put(moduleIdentifier.getFactoryName(), innerMap);
        innerMap.put(moduleIdentifier.getInstanceName(),
                new ExceptionMessageWithStackTrace(e));
        return new ValidationException(failedValidations);
    }

    public Map<String, Map<String, ExceptionMessageWithStackTrace>> getFailedValidations() {
        return failedValidations;
    }

    public static class ExceptionMessageWithStackTrace {
        private String message, stackTrace;

        public ExceptionMessageWithStackTrace() {
        }

        public ExceptionMessageWithStackTrace(String message, String stackTrace) {
            this.message = message;
            this.stackTrace = stackTrace;
        }

        public ExceptionMessageWithStackTrace(Exception e) {
            this(e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        public String getMessage() {
            return message;
        }

        public String getTrace() {
            return stackTrace;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((message == null) ? 0 : message.hashCode());
            result = prime * result
                    + ((stackTrace == null) ? 0 : stackTrace.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExceptionMessageWithStackTrace other = (ExceptionMessageWithStackTrace) obj;
            if (message == null) {
                if (other.message != null)
                    return false;
            } else if (!message.equals(other.message))
                return false;
            if (stackTrace == null) {
                if (other.stackTrace != null)
                    return false;
            } else if (!stackTrace.equals(other.stackTrace))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ExceptionMessageWithStackTrace [message=" + message
                    + ", stackTrace=" + stackTrace + "]";
        }

    }
}
