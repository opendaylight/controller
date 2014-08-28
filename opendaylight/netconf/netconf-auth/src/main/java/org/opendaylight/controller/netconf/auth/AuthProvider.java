/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.auth;

/**
 * Authentication Service definition for netconf.
 */
public interface AuthProvider {

    /**
     * Authenticate user by username/password.
     *
     * @param username username
     * @param password password
     * @return true if authentication is successful, false otherwise
     */
    boolean authenticated(String username, String password);

}
