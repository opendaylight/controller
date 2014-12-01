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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Stopwatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.netty.EchoClientHandler.State;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandler;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSHTest {
    public static final Logger logger = LoggerFactory.getLogger(SSHTest.class);
    public static final String AHOJ = "ahoj\n";
    private EventLoopGroup nettyGroup;
    HashedWheelTimer hashedWheelTimer;

    @Before
    public void setUp() throws Exception {
        hashedWheelTimer = new HashedWheelTimer();
        nettyGroup = new NioEventLoopGroup();
    }

    @After
    public void tearDown() throws Exception {
        hashedWheelTimer.stop();
        nettyGroup.shutdownGracefully();
    }

    @Test
    public void test() throws Exception {
        new Thread(new EchoServer(), "EchoServer").start();
        AuthProvider authProvider = mock(AuthProvider.class);
        doReturn(true).when(authProvider).authenticated(anyString(), anyString());
        doReturn("auth").when(authProvider).toString();

        NetconfSSHServer netconfSSHServer = NetconfSSHServer.start(10831, NetconfConfigUtil.getNetconfLocalAddress(),
                new NioEventLoopGroup(), PEMGenerator.generate().toCharArray());
        netconfSSHServer.setAuthProvider(authProvider);

        InetSocketAddress address = netconfSSHServer.getLocalSocketAddress();

        final EchoClientHandler echoClientHandler = connectClient(new InetSocketAddress("localhost", address.getPort()));

        Stopwatch stopwatch = new Stopwatch().start();
        while(echoClientHandler.isConnected() == false && stopwatch.elapsed(TimeUnit.SECONDS) < 5) {
            Thread.sleep(100);
        }
        assertTrue(echoClientHandler.isConnected());
        logger.info("connected, writing to client");
        echoClientHandler.write(AHOJ);
        // check that server sent back the same string
        stopwatch = stopwatch.reset().start();
        while (echoClientHandler.read().endsWith(AHOJ) == false && stopwatch.elapsed(TimeUnit.SECONDS) < 5) {
            Thread.sleep(100);
        }
        try {
            String read = echoClientHandler.read();
            assertTrue(read + " should end with " + AHOJ, read.endsWith(AHOJ));
        } finally {
            logger.info("Closing socket");
            netconfSSHServer.close();
            netconfSSHServer.join();
        }
    }

    public EchoClientHandler connectClient(InetSocketAddress address) {
        final EchoClientHandler echoClientHandler = new EchoClientHandler();
        ChannelInitializer<NioSocketChannel> channelInitializer = new ChannelInitializer<NioSocketChannel>() {
            @Override
            public void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addFirst(AsyncSshHandler.createForNetconfSubsystem(new LoginPassword("a", "a")));
                ch.pipeline().addLast(echoClientHandler);
            }
        };
        Bootstrap b = new Bootstrap();

        b.group(nettyGroup)
                .channel(NioSocketChannel.class)
                .handler(channelInitializer);

        // Start the client.
        b.connect(address).addListener(echoClientHandler);
        return echoClientHandler;
    }

    @Test
    public void testClientWithoutServer() throws Exception {
        InetSocketAddress address = new InetSocketAddress(12345);
        final EchoClientHandler echoClientHandler = connectClient(address);
        Stopwatch stopwatch = new Stopwatch().start();
        while(echoClientHandler.getState() == State.CONNECTING && stopwatch.elapsed(TimeUnit.SECONDS) < 5) {
            Thread.sleep(100);
        }
        assertFalse(echoClientHandler.isConnected());
        assertEquals(State.FAILED_TO_CONNECT, echoClientHandler.getState());
    }

}
