/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.virtualsocket.VirtualSocket;

/**
 * Wrapper class around GANYMED SSH java library.
 */
class SshClient {
    private final VirtualSocket socket;
    private final Map<Integer, SshSession> openSessions = new HashMap<>();
    private final AuthenticationHandler authenticationHandler;
    private Connection connection;

    public SshClient(VirtualSocket socket, AuthenticationHandler authenticationHandler) throws IOException {
        this.socket = socket;
        this.authenticationHandler = authenticationHandler;
    }

    public SshSession openSession() throws IOException {
        if (connection == null) {
            connect();
        }

        Session session = connection.openSession();
        SshSession sshSession = new SshSession(session);
        openSessions.put(openSessions.size(), sshSession);

        return sshSession;
    }

    private void connect() throws IOException {
        connection = new Connection(socket);

        connection.connect();
        authenticationHandler.authenticate(connection);
    }


    public void close() {
        for (SshSession session : openSessions.values()){
            session.close();
        }

        openSessions.clear();

        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public String toString() {
        return "SshClient{" +
                "socket=" + socket +
                '}';
    }
}
