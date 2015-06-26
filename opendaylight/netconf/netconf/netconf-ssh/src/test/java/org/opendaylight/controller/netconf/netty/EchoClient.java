/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClient extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(EchoClient.class);


    private final ChannelInitializer<LocalChannel> channelInitializer;


    public EchoClient(final ChannelHandler clientHandler) {
        channelInitializer = new ChannelInitializer<LocalChannel>() {
            @Override
            public void initChannel(LocalChannel ch) throws Exception {
                ch.pipeline().addLast(clientHandler);
            }
        };
    }

    public EchoClient(ChannelInitializer<LocalChannel> channelInitializer) {
        this.channelInitializer = channelInitializer;
    }

    @Override
    public void run() {
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
                    .channel(LocalChannel.class)
                    .handler(channelInitializer);

            // Start the client.
            LocalAddress localAddress = new LocalAddress("foo");
            ChannelFuture f = b.connect(localAddress).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            LOG.error("Error in client", e);
            throw new RuntimeException("Error in client", e);
        } finally {
            // Shut down the event loop to terminate all threads.
            LOG.info("Client is shutting down");
            group.shutdownGracefully();
        }
    }
}
