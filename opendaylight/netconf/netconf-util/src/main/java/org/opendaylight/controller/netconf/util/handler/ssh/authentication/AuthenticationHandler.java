package org.opendaylight.controller.netconf.util.handler.ssh.authentication;

import ch.ethz.ssh2.Connection;

import java.io.IOException;

/**
 * Class providing authentication facility to SSH handler.
 */
public abstract class AuthenticationHandler {
    public abstract void authenticate(Connection connection) throws IOException;
}
