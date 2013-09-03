/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerIO {
    private static final Logger logger = LoggerFactory
            .getLogger(ControllerIO.class);
    private static Short defaultOpenFlowPort = 6633;
    private Short openFlowPort;
    private InetAddress controllerIP;
    private NetworkInterface netInt;
    private SelectionKey serverSelectionKey;
    private IController listener;
    private ServerSocketChannel serverSocket;
    private Selector selector;
    private boolean running;
    private Thread controllerIOThread;

    public ControllerIO(IController l) {
        this.listener = l;
        this.openFlowPort = defaultOpenFlowPort;
        String portString = System.getProperty("of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.decode(portString).shortValue();
            } catch (NumberFormatException e) {
                logger.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort);
            }
        }
        String addressString = System.getProperty("of.address");
        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
            } catch (Exception e) {
                controllerIP = null;
                logger.warn("Invalid IP: {}, use wildcard *", addressString);
            }
        } else {
            controllerIP = null;
        }
    }

    public void start() throws IOException {
        this.running = true;
        this.netInt = null;
        controllerIOThread = new Thread(new Runnable() {
            @Override
            public void run() {
                waitUntilInterfaceUp();
                if (!startAcceptConnections()) {
                    return;
                }
                logger.info("Controller is now listening on {}:{}",
                        (controllerIP == null) ? "any" : controllerIP.getHostAddress(),
                        openFlowPort);
                boolean netInterfaceUp = true;
                while (running) {
                    try {
                        // wait for an incoming connection
                        // check interface state every 5sec
                        selector.select(5000);
                        Iterator<SelectionKey> selectedKeys = selector
                                .selectedKeys().iterator();
                        netInterfaceUp = isNetInterfaceUp(netInterfaceUp);
                        while (selectedKeys.hasNext()) {
                            SelectionKey skey = selectedKeys.next();
                            selectedKeys.remove();
                            if (skey.isValid() && skey.isAcceptable()) {
                                ((Controller) listener).handleNewConnection(
                                        selector, serverSelectionKey);
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }, "ControllerI/O Thread");
        controllerIOThread.start();
    }

    private boolean startAcceptConnections() {
        if (running) {
            try {
                // obtain a selector
                selector = SelectorProvider.provider().openSelector();
                // create the listening socket
                serverSocket = ServerSocketChannel.open();
                serverSocket.configureBlocking(false);
                serverSocket.socket().bind(
                        new java.net.InetSocketAddress(controllerIP,
                                openFlowPort));
                serverSocket.socket().setReuseAddress(true);
                // register this socket for accepting incoming
                // connections
                serverSelectionKey = serverSocket.register(selector,
                        SelectionKey.OP_ACCEPT);
            } catch (IOException e) {
                logger.error(
                        "Failed to listen on {}:{}, exit",
                        (controllerIP == null) ? "" : controllerIP
                                .getHostAddress(), openFlowPort);
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isNetInterfaceUp(boolean currentlyUp) {
        if (controllerIP == null) {
            // for wildcard address, return since there is always an "up"
            // interface (such as loopback)
            return true;
        }
        boolean up;
        try {
            if (netInt == null) {
                logger.warn("Can't find any operational interface for address {}",
                        controllerIP.getHostAddress());
                return false;
            }
            up = netInt.isUp();
            if (!up) {
                // always generate log if the interface is down
                logger.warn("Interface {} with address {} is DOWN!",
                        netInt.getDisplayName(),
                        controllerIP.getHostAddress());
            } else {
                if (!currentlyUp) {
                    // only generate log if the interface changes from down to up
                    logger.info("Interface {} with address {} is UP!",
                            netInt.getDisplayName(),
                            controllerIP.getHostAddress());
                }
            }
        } catch (SocketException e) {
            logger.warn("Interface {} with address {} is DOWN!",
                    netInt.getDisplayName(),
                    controllerIP.getHostAddress());
           up = false;
        }
        return up;
    }

    private void waitUntilInterfaceUp() {
        if (controllerIP == null) {
            // for wildcard address, return since there is always an "up"
            // interface (such as loopback)
            return;
        }
        boolean isUp = false;
        do {
            try {
                // get the network interface from the address
                netInt = NetworkInterface.getByInetAddress(controllerIP);
                isUp = isNetInterfaceUp(isUp);
                if (!isUp) {
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
            }
        } while ((!isUp) && (running));
    }
    public void shutDown() throws IOException {
        this.running = false;
        this.selector.wakeup();
        this.serverSocket.close();
    }
}
