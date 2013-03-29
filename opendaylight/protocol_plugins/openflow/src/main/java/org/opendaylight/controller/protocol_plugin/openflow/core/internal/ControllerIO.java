
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
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
                logger.warn("Invalid port:" + portString + ", use default("
                        + openFlowPort + ")");
            }
        }
    }

    public void start() throws IOException {
        this.running = true;
        // obtain a selector
        this.selector = SelectorProvider.provider().openSelector();
        // create the listening socket
        this.serverSocket = ServerSocketChannel.open();
        this.serverSocket.configureBlocking(false);
        this.serverSocket.socket().bind(
                new java.net.InetSocketAddress(openFlowPort));
        this.serverSocket.socket().setReuseAddress(true);
        // register this socket for accepting incoming connections
        this.serverSelectionKey = this.serverSocket.register(selector,
                SelectionKey.OP_ACCEPT);
        controllerIOThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        // wait for an incoming connection
                        selector.select(0);
                        Iterator<SelectionKey> selectedKeys = selector
                                .selectedKeys().iterator();
                        while (selectedKeys.hasNext()) {
                            SelectionKey skey = selectedKeys.next();
                            selectedKeys.remove();
                            if (skey.isValid() && skey.isAcceptable()) {
                                 ((Controller) listener).handleNewConnection(selector,
                                        serverSelectionKey);
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }, "ControllerI/O Thread");
        controllerIOThread.start();
        logger.info("Controller is now listening on port " + openFlowPort);
    }

    public void shutDown() throws IOException {
        this.running = false;
        this.selector.wakeup();
        this.serverSocket.close();
    }
}
