/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.threads;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.ssh2.AuthenticationResult;
import ch.ethz.ssh2.PtySettings;
import ch.ethz.ssh2.ServerAuthenticationCallback;
import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerConnectionCallback;
import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;
import ch.ethz.ssh2.SimpleServerSessionCallback;

import com.google.common.base.Supplier;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.stream.ChunkedStream;

/**
 * One instance represents per connection, responsible for ssh handshake.
 * Once auth succeeds and correct subsystem is chosen, backend connection with
 * netty netconf server is made. This task finishes right after negotiation is done.
 */
@ThreadSafe
public class Handshaker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Handshaker.class);

    private final ServerConnection ganymedConnection;
    private final String session;


    public Handshaker(Socket socket, LocalAddress localAddress, long sessionId, AuthProvider authProvider,
                      EventLoopGroup bossGroup, final char[] pem) throws IOException {

        this.session = "Session " + sessionId;

        String remoteAddressWithPort = socket.getRemoteSocketAddress().toString().replace("/", "");
        logger.debug("{} started with {}", session, remoteAddressWithPort);
        String remoteAddress, remotePort;
        if (remoteAddressWithPort.contains(":")) {
            String[] split = remoteAddressWithPort.split(":");
            remoteAddress = split[0];
            remotePort = split[1];
        } else {
            remoteAddress = remoteAddressWithPort;
            remotePort = "";
        }
        ServerAuthenticationCallbackImpl serverAuthenticationCallback = new ServerAuthenticationCallbackImpl(
                authProvider, session);

        ganymedConnection = new ServerConnection(socket);

        ServerConnectionCallbackImpl serverConnectionCallback = new ServerConnectionCallbackImpl(
                serverAuthenticationCallback, remoteAddress, remotePort, session,
                getGanymedAutoCloseable(ganymedConnection), localAddress, bossGroup);

        // initialize ganymed
        ganymedConnection.setPEMHostKey(pem, null);
        ganymedConnection.setAuthenticationCallback(serverAuthenticationCallback);
        ganymedConnection.setServerConnectionCallback(serverConnectionCallback);
    }


    private static AutoCloseable getGanymedAutoCloseable(final ServerConnection ganymedConnection) {
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                ganymedConnection.close();
            }
        };
    }

    @Override
    public void run() {
        // let ganymed process handshake
        logger.trace("{} is started", session);
        try {
            // TODO this should be guarded with a timer to prevent resource exhaustion
            ganymedConnection.connect();
        } catch (IOException e) {
            logger.debug("{} connection error", session, e);
        }
        logger.trace("{} is exiting", session);
    }
}

/**
 * Netty client handler that forwards bytes from backed server to supplied output stream.
 * When backend server closes the connection, remoteConnection.close() is called to tear
 * down ssh connection.
 */
class SSHClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SSHClientHandler.class);
    private final AutoCloseable remoteConnection;
    private final BufferedOutputStream remoteOutputStream;
    private final String session;
    private ChannelHandlerContext channelHandlerContext;

    public SSHClientHandler(AutoCloseable remoteConnection, OutputStream remoteOutputStream,
                            String session) {
        this.remoteConnection = remoteConnection;
        this.remoteOutputStream = new BufferedOutputStream(remoteOutputStream);
        this.session = session;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channelHandlerContext = ctx;
        logger.debug("{} Client active", session);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf bb = (ByteBuf) msg;
        // we can block the server here so that slow client does not cause memory pressure
        try {
            bb.forEachByte(new ByteBufProcessor() {
                @Override
                public boolean process(byte value) throws Exception {
                    remoteOutputStream.write(value);
                    return true;
                }
            });
        } finally {
            bb.release();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws IOException {
        logger.trace("{} Flushing", session);
        remoteOutputStream.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        logger.warn("{} Unexpected exception from downstream", session, cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("{} channelInactive() called, closing remote client ctx", session);
        remoteConnection.close();//this should close socket and all threads created for this client
        this.channelHandlerContext = null;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return checkNotNull(channelHandlerContext, "Channel is not active");
    }
}

/**
 * Ganymed handler that gets unencrypted input and output streams, connects them to netty.
 * Checks that 'netconf' subsystem is chosen by user.
 * Launches new ClientInputStreamPoolingThread thread once session is established.
 * Writes custom header to netty server, to inform it about IP address and username.
 */
class ServerConnectionCallbackImpl implements ServerConnectionCallback {
    private static final Logger logger = LoggerFactory.getLogger(ServerConnectionCallbackImpl.class);
    public static final String NETCONF_SUBSYSTEM = "netconf";

    private final Supplier<String> currentUserSupplier;
    private final String remoteAddress;
    private final String remotePort;
    private final String session;
    private final AutoCloseable ganymedConnection;
    private final LocalAddress localAddress;
    private final EventLoopGroup bossGroup;

    ServerConnectionCallbackImpl(Supplier<String> currentUserSupplier, String remoteAddress, String remotePort, String session,
                                 AutoCloseable ganymedConnection, LocalAddress localAddress, EventLoopGroup bossGroup) {
        this.currentUserSupplier = currentUserSupplier;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.session = session;
        this.ganymedConnection = ganymedConnection;
        // initialize netty local connection
        this.localAddress = localAddress;
        this.bossGroup = bossGroup;
    }

    private static ChannelFuture initializeNettyConnection(LocalAddress localAddress, EventLoopGroup bossGroup,
                                                           final SSHClientHandler sshClientHandler) {
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(bossGroup).channel(LocalChannel.class);

        clientBootstrap.handler(new ChannelInitializer<LocalChannel>() {
            @Override
            public void initChannel(LocalChannel ch) throws Exception {
                ch.pipeline().addLast(sshClientHandler);
            }
        });
        // asynchronously initialize local connection to netconf server
        return clientBootstrap.connect(localAddress);
    }

    @Override
    public ServerSessionCallback acceptSession(final ServerSession serverSession) {
        String currentUser = currentUserSupplier.get();
        final String additionalHeader = new NetconfHelloMessageAdditionalHeader(currentUser, remoteAddress,
                remotePort, "ssh", "client").toFormattedString();


        return new SimpleServerSessionCallback() {
            @Override
            public Runnable requestSubsystem(final ServerSession ss, final String subsystem) throws IOException {
                return new Runnable() {
                    @Override
                    public void run() {
                        if (NETCONF_SUBSYSTEM.equals(subsystem)) {
                            // connect
                            final SSHClientHandler sshClientHandler = new SSHClientHandler(ganymedConnection, ss.getStdin(), session);
                            ChannelFuture clientChannelFuture = initializeNettyConnection(localAddress, bossGroup, sshClientHandler);
                            // get channel
                            final Channel channel = clientChannelFuture.awaitUninterruptibly().channel();

                            // write additional header before polling thread is started
                            // polling thread could process and forward data before additional header is written
                            // This will result into unexpected state:  hello message without additional header and the next message with additional header
                            channel.writeAndFlush(Unpooled.copiedBuffer(additionalHeader.getBytes()));

                            new ClientInputStreamPoolingThread(session, ss.getStdout(), channel, new AutoCloseable() {
                                @Override
                                public void close() throws Exception {
                                    logger.trace("Closing both ganymed and local connection");
                                    try {
                                        ganymedConnection.close();
                                    } catch (Exception e) {
                                        logger.warn("Ignoring exception while closing ganymed", e);
                                    }
                                    try {
                                        channel.close();
                                    } catch (Exception e) {
                                        logger.warn("Ignoring exception while closing channel", e);
                                    }
                                }
                            }, sshClientHandler.getChannelHandlerContext()).start();
                        } else {
                            logger.debug("{} Wrong subsystem requested:'{}', closing ssh session", serverSession, subsystem);
                            String reason = "Only netconf subsystem is supported, requested:" + subsystem;
                            closeSession(ss, reason);
                        }
                    }
                };
            }

            public void closeSession(ServerSession ss, String reason) {
                logger.trace("{} Closing session - {}", serverSession, reason);
                try {
                    ss.getStdin().write(reason.getBytes());
                } catch (IOException e) {
                    logger.warn("{} Exception while closing session", serverSession, e);
                }
                ss.close();
            }

            @Override
            public Runnable requestPtyReq(final ServerSession ss, final PtySettings pty) throws IOException {
                return new Runnable() {
                    @Override
                    public void run() {
                        closeSession(ss, "PTY request not supported");
                    }
                };
            }

            @Override
            public Runnable requestShell(final ServerSession ss) throws IOException {
                return new Runnable() {
                    @Override
                    public void run() {
                        closeSession(ss, "Shell not supported");
                    }
                };
            }
        };
    }
}

/**
 * Only thread that is required during ssh session, forwards client's input to netty.
 * When user closes connection, onEndOfInput.close() is called to tear down the local channel.
 */
class ClientInputStreamPoolingThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ClientInputStreamPoolingThread.class);

    private final InputStream fromClientIS;
    private final Channel serverChannel;
    private final AutoCloseable onEndOfInput;
    private final ChannelHandlerContext channelHandlerContext;

    ClientInputStreamPoolingThread(String session, InputStream fromClientIS, Channel serverChannel, AutoCloseable onEndOfInput,
                                   ChannelHandlerContext channelHandlerContext) {
        super(ClientInputStreamPoolingThread.class.getSimpleName() + " " + session);
        this.fromClientIS = fromClientIS;
        this.serverChannel = serverChannel;
        this.onEndOfInput = onEndOfInput;
        this.channelHandlerContext = channelHandlerContext;
    }

    @Override
    public void run() {
        ChunkedStream chunkedStream = new ChunkedStream(fromClientIS);
        try {
            ByteBuf byteBuf;
            while ((byteBuf = chunkedStream.readChunk(channelHandlerContext/*only needed for ByteBuf alloc */)) != null) {
                serverChannel.writeAndFlush(byteBuf);
            }
        } catch (Exception e) {
            logger.warn("Exception", e);
        } finally {
            logger.trace("End of input");
            // tear down connection
            try {
                onEndOfInput.close();
            } catch (Exception e) {
                logger.warn("Ignoring exception while closing socket", e);
            }
        }
    }
}

/**
 * Authentication handler for ganymed.
 * Provides current user name after authenticating using supplied AuthProvider.
 */
@NotThreadSafe
class ServerAuthenticationCallbackImpl implements ServerAuthenticationCallback, Supplier<String> {
    private static final Logger logger = LoggerFactory.getLogger(ServerAuthenticationCallbackImpl.class);
    private final AuthProvider authProvider;
    private final String session;
    private String currentUser;

    ServerAuthenticationCallbackImpl(AuthProvider authProvider, String session) {
        this.authProvider = authProvider;
        this.session = session;
    }

    @Override
    public String initAuthentication(ServerConnection sc) {
        logger.trace("{} Established connection", session);
        return "Established connection" + "\r\n";
    }

    @Override
    public String[] getRemainingAuthMethods(ServerConnection sc) {
        return new String[]{ServerAuthenticationCallback.METHOD_PASSWORD};
    }

    @Override
    public AuthenticationResult authenticateWithNone(ServerConnection sc, String username) {
        return AuthenticationResult.FAILURE;
    }

    @Override
    public AuthenticationResult authenticateWithPassword(ServerConnection sc, String username, String password) {
        checkState(currentUser == null);
        try {
            if (authProvider.authenticated(username, password)) {
                currentUser = username;
                logger.trace("{} user {} authenticated", session, currentUser);
                return AuthenticationResult.SUCCESS;
            }
        } catch (Exception e) {
            logger.warn("{} Authentication failed", session, e);
        }
        return AuthenticationResult.FAILURE;
    }

    @Override
    public AuthenticationResult authenticateWithPublicKey(ServerConnection sc, String username, String algorithm,
                                                          byte[] publicKey, byte[] signature) {
        return AuthenticationResult.FAILURE;
    }

    @Override
    public String get() {
        return currentUser;
    }
}
