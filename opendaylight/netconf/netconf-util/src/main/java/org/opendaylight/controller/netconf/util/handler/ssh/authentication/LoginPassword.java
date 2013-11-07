/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.authentication;

import ch.ethz.ssh2.Connection;

import java.io.IOException;

/**
 * Class Providing username/password authentication option to {@link org.opendaylight.controller.netconf.util.handler.ssh.SshHandler}
 */
public class LoginPassword extends AuthenticationHandler {
    private final String username;
    private final String password;

    public LoginPassword(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void authenticate(Connection connection) throws IOException {
        boolean isAuthenticated = connection.authenticateWithPassword(username, password);

        if (isAuthenticated == false) throw new IOException("Authentication failed.");
    }
}
