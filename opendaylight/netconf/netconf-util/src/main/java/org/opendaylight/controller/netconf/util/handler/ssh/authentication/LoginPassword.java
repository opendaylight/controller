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
