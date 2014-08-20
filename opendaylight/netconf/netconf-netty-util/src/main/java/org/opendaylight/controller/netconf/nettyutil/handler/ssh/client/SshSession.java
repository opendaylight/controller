/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import ch.ethz.ssh2.Session;
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

    public SshSession(final Session session) {
        this.session = session;
    }

    public void startSubSystem(final String name) throws IOException {
        session.startSubSystem(name);
    }

    public InputStream getStdout() {
        return session.getStdout();
    }

    // FIXME according to http://www.ganymed.ethz.ch/ssh2/FAQ.html#blocking you should read data from both stdout and stderr to prevent window filling up (since stdout and stderr share a window)
    // FIXME stdErr is not used anywhere
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
