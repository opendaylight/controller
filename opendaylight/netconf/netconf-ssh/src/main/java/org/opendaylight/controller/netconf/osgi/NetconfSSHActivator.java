/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.osgi;

import com.google.common.base.Optional;
import java.net.InetSocketAddress;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for netconf SSH bundle which creates SSH bridge between netconf client and netconf server. Activator
 * starts SSH Server in its own thread. This thread is closed when activator calls stop() method. Server opens socket
 * and listen for client connections. Each client connection creation is handled in separate
 * {@link org.opendaylight.controller.netconf.ssh.threads.SocketThread} thread.
 * This thread creates two additional threads {@link org.opendaylight.controller.netconf.ssh.threads.IOThread}
 * forwarding data from/to client.IOThread closes servers session and server connection when it gets -1 on input stream.
 * {@link org.opendaylight.controller.netconf.ssh.threads.IOThread}'s run method waits for -1 on input stream to finish.
 * All threads are daemons.
 **/
public class NetconfSSHActivator implements BundleActivator{

    private NetconfSSHServer server;
    private static final Logger logger =  LoggerFactory.getLogger(NetconfSSHActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {

        logger.trace("Starting netconf SSH  bridge.");

        Optional<InetSocketAddress> sshSocketAddressOptional = NetconfConfigUtil.extractSSHNetconfAddress(context);
        InetSocketAddress tcpSocketAddress = NetconfConfigUtil.extractTCPNetconfAddress(context,
                "TCP is not configured, netconf ssh bridge is not available.");

        if (sshSocketAddressOptional.isPresent()){
            server = NetconfSSHServer.start(sshSocketAddressOptional.get().getPort(),tcpSocketAddress);
            Thread serverThread = new  Thread(server,"netconf SSH server thread");
            serverThread.setDaemon(true);
            serverThread.start();
            logger.trace("Netconf SSH  bridge up and running.");
        } else {
            logger.trace("No valid connection configuration for SSH bridge found.");
            throw new Exception("No valid connection configuration for SSH bridge found.");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (server != null){
            logger.trace("Netconf SSH bridge going down ...");
            server.stop();
            logger.trace("Netconf SSH bridge is down ...");
        }
    }
}
