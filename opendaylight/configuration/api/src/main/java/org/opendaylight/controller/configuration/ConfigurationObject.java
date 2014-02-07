/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration;

import java.io.Serializable;

public abstract class ConfigurationObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_REGEX = "^[\\w-=\\+\\*\\.\\(\\)\\[\\]\\@\\|\\:]{1,256}$";
    private static final String REGEX_PROP_NAME = "resourceNameRegularExpression";
    private static String regex;

    static {
        String customRegex = System.getProperty(REGEX_PROP_NAME);
        regex = (customRegex != null) ? customRegex : DEFAULT_REGEX;
    }

    /**
     * Checks if the provided resource name matches the controller resource name
     * regular expression
     *
     * @param name
     *            The resource name to test
     * @return true if the resource name is not null and matches the controller
     *         resource name regular expression, false otherwise
     */
    protected boolean isValidResourceName(String name) {
        return name != null && name.matches(regex);
    }

    /**
     * Return the regular expression currently in use for testing the controller
     * resource names
     *
     * @return The regular expression
     */
    public static String getRegularExpression() {
        return regex;
    }
}
