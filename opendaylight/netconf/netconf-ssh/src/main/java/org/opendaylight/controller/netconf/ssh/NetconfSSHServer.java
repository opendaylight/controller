/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh;

import java.net.ServerSocket;

public class NetconfSSHServer  {

    private static boolean acceptMore = true;
    private static final int SERVER_PORT = 830;
    private ServerSocket ss = null;

    private NetconfSSHServer() throws Exception{
        this.ss = new ServerSocket(SERVER_PORT);
        while (acceptMore) {
            SocketThread.start(ss.accept());
        }
    }
    public static NetconfSSHServer start() throws Exception {
        return new NetconfSSHServer();
    }

    public void stop() throws Exception {
           ss.close();
    }

}
