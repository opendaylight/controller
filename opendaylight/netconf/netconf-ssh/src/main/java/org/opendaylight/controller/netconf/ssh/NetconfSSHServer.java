/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.netconf.ssh.threads.SocketThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class NetconfSSHServer implements Runnable {

    private static boolean acceptMore = true;
    private ServerSocket ss = null;
    private static final Logger logger =  LoggerFactory.getLogger(NetconfSSHServer.class);
    private static final AtomicLong sesssionId = new AtomicLong();
    private final InetSocketAddress clientAddress;

    private NetconfSSHServer(int serverPort,InetSocketAddress clientAddress) throws Exception{

        logger.trace("Creating SSH server socket on port {}",serverPort);
        this.ss = new ServerSocket(serverPort);
        if (!ss.isBound()){
            throw new Exception("Socket can't be bound to requested port :"+serverPort);
        }
        logger.trace("Server socket created.");
        this.clientAddress = clientAddress;

    }


    public static NetconfSSHServer start(int serverPort, InetSocketAddress clientAddress) throws Exception {
        return new NetconfSSHServer(serverPort, clientAddress);
    }

    public void stop() throws Exception {
        acceptMore = false;
        logger.trace("Closing SSH server socket.");
        ss.close();
        logger.trace("SSH server socket closed.");
    }

    @Override
    public void run() {
        while (acceptMore) {
            logger.trace("Starting new socket thread.");
            try {
               SocketThread.start(ss.accept(), clientAddress, sesssionId.incrementAndGet());
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }
}
