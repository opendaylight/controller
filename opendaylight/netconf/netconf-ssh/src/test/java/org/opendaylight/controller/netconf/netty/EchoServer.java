/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Echoes back any received data from a client.
 */
public class EchoServer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(EchoServer.class);

    public void run() {
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(LocalServerChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        public void initChannel(LocalChannel ch) throws Exception {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });

            // Start the server.
            LocalAddress localAddress = NetconfConfigUtil.getNetconfLocalAddress();
            ChannelFuture f = b.bind(localAddress).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Thread(new EchoServer()).start();
        Thread.sleep(1000);
        EchoClientHandler clientHandler = new EchoClientHandler();
        EchoClient echoClient = new EchoClient(clientHandler);
        new Thread(echoClient).start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        do {
            String message = reader.readLine();
            if (message == null ||  "exit".equalsIgnoreCase(message)) {
                break;
            }
            LOG.debug("Got '{}'", message);
            clientHandler.write(message);
        } while (true);
        System.exit(0);
    }
}
