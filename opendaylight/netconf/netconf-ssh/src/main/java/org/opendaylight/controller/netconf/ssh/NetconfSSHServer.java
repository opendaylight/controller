/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.ssh.threads.Handshaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;

/**
 * Thread that accepts client connections. Accepted socket is forwarded to {@link org.opendaylight.controller.netconf.ssh.threads.Handshaker},
 * which is executed in {@link #handshakeExecutor}.
 */
@ThreadSafe
public final class NetconfSSHServer extends Thread implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetconfSSHServer.class);
    private static final AtomicLong SESSION_ID_COUNTER = new AtomicLong();

    private final ServerSocket serverSocket;
    private final LocalAddress localAddress;
    private final EventLoopGroup bossGroup;
    private Optional<AuthProvider> authProvider = Optional.absent();
    private final ExecutorService handshakeExecutor;
    private final char[] pem;
    private volatile boolean up;

    private NetconfSSHServer(final int serverPort, final LocalAddress localAddress, final EventLoopGroup bossGroup, final char[] pem) throws IOException {
        super(NetconfSSHServer.class.getSimpleName());
        this.bossGroup = bossGroup;
        this.pem = pem;
        LOGGER.trace("Creating SSH server socket on port {}", serverPort);
        this.serverSocket = new ServerSocket(serverPort);
        if (serverSocket.isBound() == false) {
            throw new IllegalStateException("Socket can't be bound to requested port :" + serverPort);
        }
        LOGGER.trace("Server socket created.");
        this.localAddress = localAddress;
        this.up = true;
        handshakeExecutor = Executors.newFixedThreadPool(10);
    }

    public static NetconfSSHServer start(final int serverPort, final LocalAddress localAddress, final EventLoopGroup bossGroup, final char[] pemArray) throws IOException {
        final NetconfSSHServer netconfSSHServer = new NetconfSSHServer(serverPort, localAddress, bossGroup, pemArray);
        netconfSSHServer.start();
        return netconfSSHServer;
    }

    public synchronized AuthProvider getAuthProvider() {
        Preconditions.checkState(authProvider.isPresent(), "AuthenticationProvider is not set up, cannot authenticate user");
        return authProvider.get();
    }

    public synchronized void setAuthProvider(final AuthProvider authProvider) {
        if(this.authProvider != null) {
            LOGGER.debug("Changing auth provider to {}", authProvider);
        }
        this.authProvider = Optional.fromNullable(authProvider);
    }

    @Override
    public void close() throws IOException {
        up = false;
        LOGGER.trace("Closing SSH server socket.");
        serverSocket.close();
        bossGroup.shutdownGracefully();
        LOGGER.trace("SSH server socket closed.");
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
            } catch (final IOException e) {
                if (up == false) {
                    LOGGER.trace("Exiting server thread", e);
                } else {
                    LOGGER.warn("Exception occurred during socket.accept", e);
                }
            }
            if (acceptedSocket != null) {
                try {
                    final Handshaker task = new Handshaker(acceptedSocket, localAddress, SESSION_ID_COUNTER.incrementAndGet(), getAuthProvider(), bossGroup, pem);
                    handshakeExecutor.submit(task);
                } catch (final IOException e) {
                    LOGGER.warn("Cannot set PEMHostKey, closing connection", e);
                    closeSocket(acceptedSocket);
                } catch (final IllegalStateException e) {
                    LOGGER.warn("Cannot accept connection, closing", e);
                    closeSocket(acceptedSocket);
                }
            }
        }
        LOGGER.debug("Server thread is exiting");
    }

    private void closeSocket(final Socket acceptedSocket) {
        try {
            acceptedSocket.close();
        } catch (final IOException e) {
            LOGGER.warn("Ignoring exception while closing socket", e);
        }
    }

}
