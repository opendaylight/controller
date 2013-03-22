
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

/**
 * IAAAProvider exposes a pluggable interface for 3rd party Authentication and Authorization
 * providers to support the UserManager with AAA management.
 */

public interface IAAAProvider {

    /**
     * Authenticate user with AAA server and return authentication and authorization info
     * using the Provider's mechanism
     * @param userName
     * @param password
     * @param server
     * @param secretKey
     * @return Authentication and Authorization Response
     */
    public AuthResponse authService(String userName, String password,
            String server, String secretKey);

    /**
     * Returns the Name of the Provider
     *
     * @return Name of the AAA provider
     */
    public String getName();
}
