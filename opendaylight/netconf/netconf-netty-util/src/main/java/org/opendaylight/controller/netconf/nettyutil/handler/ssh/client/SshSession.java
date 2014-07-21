/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import ch.ethz.ssh2.channel.Channel;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper class for proprietary SSH sessions implementations
 */
class SshSession implements Closeable {
    private final Session session;

    public SshSession(Session session) {
        this.session = session;
    }


    public void startSubSystem(String name) throws IOException {
        session.startSubSystem(name);
    }

    public InputStream getStdout() {
        return new StreamGobbler(session.getStdout());
    }

    public InputStream getStderr() {
        return session.getStderr();
    }

    public OutputStream getStdin() {
        return session.getStdin();
    }

    @Override
    public void close() {
        if (session.getState() == Channel.STATE_OPEN || session.getState() == Channel.STATE_OPENING) {
            session.close();
        }
    }
}
