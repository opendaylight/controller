/*
 * Copyright (c) 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import java.io.IOException;
import java.util.HashMap;

import org.apache.sshd.SshClient;
import org.apache.sshd.common.FactoryManager;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;

public class AsyncSshUtils {

    public static final String SUBSYSTEM = "netconf";

    public static final SshClient DEFAULT_CLIENT = SshClient.setUpDefaultClient();

    public static final int SSH_DEFAULT_NIO_WORKERS = 8;

    // Disable default timeouts from mina sshd
    private static final long DEFAULT_TIMEOUT = -1L;

    static {
        DEFAULT_CLIENT.setProperties(new HashMap<String, String>(){
            {
                put(FactoryManager.AUTH_TIMEOUT, Long.toString(DEFAULT_TIMEOUT));
                put(FactoryManager.IDLE_TIMEOUT, Long.toString(DEFAULT_TIMEOUT));
            }
        });
        // TODO make configurable, or somehow reuse netty threadpool
        DEFAULT_CLIENT.setNioWorkers(SSH_DEFAULT_NIO_WORKERS);
        DEFAULT_CLIENT.start();
    }


    public static AsyncSshHandler createForNetconfSubsystem(final AuthenticationHandler authenticationHandler) throws IOException {
        return new AsyncSshHandler(authenticationHandler);
    }
}
