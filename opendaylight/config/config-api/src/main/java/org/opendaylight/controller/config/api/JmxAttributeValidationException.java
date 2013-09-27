/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.util.Arrays;
import java.util.List;

/**
 * Exception that can be thrown during validation phase. This allows for
 * pointing user to a specific list of parameters that fail the validation. Note
 * that {@link org.opendaylight.controller.config.spi.Module#validate()} can
 * throw any runtime exception to trigger validation failure.
 */
public class JmxAttributeValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final List<JmxAttribute> attributeNames;

    public JmxAttributeValidationException(JmxAttribute jmxAttribute) {
        this(Arrays.asList(jmxAttribute));
    }

    public JmxAttributeValidationException(List<JmxAttribute> jmxAttribute) {
        this.attributeNames = jmxAttribute;
    }

    public JmxAttributeValidationException(String message,
            JmxAttribute jmxAttribute) {
        this(message, Arrays.asList(jmxAttribute));
    }

    public JmxAttributeValidationException(String message,
            List<JmxAttribute> jmxAttributes) {
        super(message);
        this.attributeNames = jmxAttributes;
    }

    public JmxAttributeValidationException(String message, Throwable cause,
            JmxAttribute jmxAttribute) {
        this(message, cause, Arrays.asList(jmxAttribute));
    }

    public JmxAttributeValidationException(String message, Throwable cause,
            List<JmxAttribute> jmxAttributes) {
        super(message, cause);
        this.attributeNames = jmxAttributes;
    }

    public List<JmxAttribute> getAttributeNames() {
        return attributeNames;
    }

    public static <T> T checkNotNull(T param, JmxAttribute jmxAttribute) {
        String message = "is null";
        return checkNotNull(param, message, jmxAttribute);
    }

    public static <T> T checkNotNull(T param, String message,
            JmxAttribute jmxAttribute) {
        if (param == null) {
            throw new JmxAttributeValidationException(
                    jmxAttribute.getAttributeName() + " " + message,
                    jmxAttribute);
        }
        return param;
    }

    public static JmxAttributeValidationException wrap(Throwable throwable,
            JmxAttribute jmxAttribute) throws JmxAttributeValidationException {
        return wrap(throwable, throwable.getMessage(), jmxAttribute);
    }

    public static JmxAttributeValidationException wrap(Throwable throwable,
            String message, JmxAttribute jmxAttribute) {

        throw new JmxAttributeValidationException(
                jmxAttribute.getAttributeName() + " " + message, throwable,
                jmxAttribute);
    }

    public static void checkCondition(boolean condition, String message,
            JmxAttribute jmxAttribute) throws JmxAttributeValidationException {
        if (condition == false) {
            throw new JmxAttributeValidationException(
                    jmxAttribute.getAttributeName() + " " + message,
                    jmxAttribute);
        }
    }
}
