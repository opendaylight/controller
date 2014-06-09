/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.netconf.netty.EchoClientHandler.State;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandler;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSHTest {
    private static final Logger LOG = LoggerFactory.getLogger(SSHTest.class);
    public static final String AHOJ = "ahoj\n";

    private static EventLoopGroup nettyGroup;
    private static HashedWheelTimer hashedWheelTimer;
    private static ExecutorService nioExec;
    private static ScheduledExecutorService minaTimerEx;

    @BeforeClass
    public static void setUp() throws Exception {
        hashedWheelTimer = new HashedWheelTimer();
        nettyGroup = new NioEventLoopGroup();
        nioExec = Executors.newFixedThreadPool(1);
        minaTimerEx = Executors.newScheduledThreadPool(1);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        hashedWheelTimer.stop();
        nettyGroup.shutdownGracefully().await();
        minaTimerEx.shutdownNow();
        nioExec.shutdownNow();
    }

    @Test
    public void test() throws Exception {
        File sshKeyPair = Files.createTempFile("sshKeyPair", ".pem").toFile();
        sshKeyPair.deleteOnExit();
        new Thread(new EchoServer(), "EchoServer").start();

        final InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 10831);
        final SshProxyServer sshProxyServer = new SshProxyServer(minaTimerEx, nettyGroup, nioExec);
        sshProxyServer.bind(
                new SshProxyServerConfigurationBuilder().setBindingAddress(addr).setLocalAddress(NetconfConfigUtil.getNetconfLocalAddress()).setAuthenticator(new PasswordAuthenticator() {
                    @Override
                    public boolean authenticate(final String username, final String password, final ServerSession session) {
                        return true;
                    }
                }).setKeyPairProvider(new PEMGeneratorHostKeyProvider(sshKeyPair.toPath().toAbsolutePath().toString())).setIdleTimeout(Integer.MAX_VALUE).createSshProxyServerConfiguration());

        final EchoClientHandler echoClientHandler = connectClient(addr);

        Stopwatch stopwatch = Stopwatch.createStarted();
        while(echoClientHandler.isConnected() == false && stopwatch.elapsed(TimeUnit.SECONDS) < 30) {
            Thread.sleep(500);
        }
        assertTrue(echoClientHandler.isConnected());
        LOG.info("connected, writing to client");
        echoClientHandler.write(AHOJ);

        // check that server sent back the same string
        stopwatch = stopwatch.reset().start();
        while (echoClientHandler.read().endsWith(AHOJ) == false && stopwatch.elapsed(TimeUnit.SECONDS) < 30) {
            Thread.sleep(500);
        }

        try {
            final String read = echoClientHandler.read();
            assertTrue(read + " should end with " + AHOJ, read.endsWith(AHOJ));
        } finally {
            LOG.info("Closing socket");
            sshProxyServer.close();
        }
    }

    public EchoClientHandler connectClient(final InetSocketAddress address) {
        final EchoClientHandler echoClientHandler = new EchoClientHandler();
        final ChannelInitializer<NioSocketChannel> channelInitializer = new ChannelInitializer<NioSocketChannel>() {
            @Override
            public void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline().addFirst(AsyncSshHandler.createForNetconfSubsystem(new LoginPassword("a", "a")));
                ch.pipeline().addLast(echoClientHandler);
            }
        };
        final Bootstrap b = new Bootstrap();

        b.group(nettyGroup)
                .channel(NioSocketChannel.class)
                .handler(channelInitializer);

        // Start the client.
        b.connect(address).addListener(echoClientHandler);
        return echoClientHandler;
    }

    @Test
    public void testClientWithoutServer() throws Exception {
        final InetSocketAddress address = new InetSocketAddress(12345);
        final EchoClientHandler echoClientHandler = connectClient(address);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        while(echoClientHandler.getState() == State.CONNECTING && stopwatch.elapsed(TimeUnit.SECONDS) < 5) {
            Thread.sleep(100);
        }
        assertFalse(echoClientHandler.isConnected());
        assertEquals(State.FAILED_TO_CONNECT, echoClientHandler.getState());
    }

}
