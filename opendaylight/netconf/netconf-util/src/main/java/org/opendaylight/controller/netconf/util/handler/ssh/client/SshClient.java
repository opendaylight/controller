/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.client;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.channel.Channel;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.virtualsocket.VirtualSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Wrapper class around GANYMED SSH java library.
 */
public class SshClient {
    private final VirtualSocket socket;
    private final Map<Integer, SshSession> openSessions = new HashMap();
    private final AuthenticationHandler authenticationHandler;
    private Connection connection;

    public SshClient(VirtualSocket socket,
                     AuthenticationHandler authenticationHandler) throws IOException {
        this.socket = socket;
        this.authenticationHandler = authenticationHandler;
    }

    public SshSession openSession() throws IOException {
        if(connection == null) connect();

        Session session =  connection.openSession();
        SshSession sshSession = new SshSession(session);
        openSessions.put(openSessions.size(), sshSession);

        return sshSession;
    }

    private void connect() throws IOException {
        connection = new Connection(socket);
        connection.connect();
        authenticationHandler.authenticate(connection);
    }

    public void closeSession(SshSession session) {
        if(   session.getState() == Channel.STATE_OPEN
           || session.getState() == Channel.STATE_OPENING) {
            session.session.close();
        }
    }

    public void close() {
        for(SshSession session : openSessions.values()) closeSession(session);

        openSessions.clear();

        if(connection != null) connection.close();
    }
}
