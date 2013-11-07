/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.client;

import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper class for proprietary SSH sessions implementations
 */
public class SshSession {
    final Session session;

    public SshSession(Session session) {
        this.session = session;
    }

    public void execCommand(String cmd) throws IOException {
        session.execCommand(cmd);
    }

    public void execCommand(String cmd, String charsetName) throws IOException {
        session.execCommand(cmd, charsetName);
    }

    public void startShell() throws IOException {
        session.startShell();
    }

    public void startSubSystem(String name) throws IOException {
        session.startSubSystem(name);
    }

    public int getState() {
        return session.getState();
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

    public int waitUntilDataAvailable(long timeout) throws IOException {
        return session.waitUntilDataAvailable(timeout);
    }

    public int waitForCondition(int condition_set, long timeout) {
        return session.waitForCondition(condition_set, timeout);
    }

    public Integer getExitStatus() {
        return session.getExitStatus();
    }

    public String getExitSignal() {
        return session.getExitSignal();
    }
}
