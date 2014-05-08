/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh;

import com.google.common.annotations.VisibleForTesting;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.threads.Handshaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that accepts client connections. Accepted socket is forwarded to {@link org.opendaylight.controller.netconf.ssh.threads.Handshaker},
 * which is executed in {@link #handshakeExecutor}.
 */
@ThreadSafe
public final class NetconfSSHServer extends Thread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(NetconfSSHServer.class);
    private static final AtomicLong sessionIdCounter = new AtomicLong();

    private final ServerSocket serverSocket;
    private final LocalAddress localAddress;
    private final EventLoopGroup bossGroup;
    private final AuthProvider authProvider;
    private final ExecutorService handshakeExecutor;
    private volatile boolean up;

    private NetconfSSHServer(int serverPort, LocalAddress localAddress, AuthProvider authProvider, EventLoopGroup bossGroup) throws IOException {
        super(NetconfSSHServer.class.getSimpleName());
        this.bossGroup = bossGroup;
        logger.trace("Creating SSH server socket on port {}", serverPort);
        this.serverSocket = new ServerSocket(serverPort);
        if (serverSocket.isBound() == false) {
            throw new IllegalStateException("Socket can't be bound to requested port :" + serverPort);
        }
        logger.trace("Server socket created.");
        this.localAddress = localAddress;
        this.authProvider = authProvider;
        this.up = true;
        handshakeExecutor = Executors.newFixedThreadPool(10);
    }

    public static NetconfSSHServer start(int serverPort, LocalAddress localAddress, AuthProvider authProvider, EventLoopGroup bossGroup) throws IOException {
        NetconfSSHServer netconfSSHServer = new NetconfSSHServer(serverPort, localAddress, authProvider, bossGroup);
        netconfSSHServer.start();
        return netconfSSHServer;
    }

    @Override
    public void close() throws IOException {
        up = false;
        logger.trace("Closing SSH server socket.");
        serverSocket.close();
        bossGroup.shutdownGracefully();
        logger.trace("SSH server socket closed.");
    }

    @VisibleForTesting
    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) serverSocket.getLocalSocketAddress();
    }

    @Override
    public void run() {
        while (up) {
            Socket acceptedSocket = null;
            try {
                acceptedSocket = serverSocket.accept();
            } catch (IOException e) {
                if (up == false) {
                    logger.trace("Exiting server thread", e);
                } else {
                    logger.warn("Exception occurred during socket.accept", e);
                }
            }
            if (acceptedSocket != null) {
                try {
                    Handshaker task = new Handshaker(acceptedSocket, localAddress, sessionIdCounter.incrementAndGet(), authProvider, bossGroup);
                    handshakeExecutor.submit(task);
                } catch (IOException e) {
                    logger.warn("Cannot set PEMHostKey, closing connection", e);
                    try {
                        acceptedSocket.close();
                    } catch (IOException e1) {
                        logger.warn("Ignoring exception while closing socket", e);
                    }
                }
            }
        }
        logger.debug("Server thread is exiting");
    }
}
