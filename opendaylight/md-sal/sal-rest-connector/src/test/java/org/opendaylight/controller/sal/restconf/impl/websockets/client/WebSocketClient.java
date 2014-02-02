/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.websockets.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClient  {

    private final URI uri;
    private Bootstrap bootstrap = new Bootstrap();;
    private final WebSocketClientHandler clientHandler;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);
    private Channel clientChannel;
    private final EventLoopGroup group = new NioEventLoopGroup();

    public WebSocketClient(URI uri,IClientMessageCallback clientMessageCallback) {
        this.uri = uri;
        clientHandler = new WebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, false,null),clientMessageCallback); // last null could be replaced with DefaultHttpHeaders
        initialize();
    }
    private void initialize(){

        String protocol = uri.getScheme();
        if (!"http".equals(protocol)) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
                        pipeline.addLast("ws-handler", clientHandler);
                    }
                });
    }
    public void connect() throws InterruptedException{
        System.out.println("WebSocket Client connecting");
        clientChannel  = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
        clientHandler.handshakeFuture().sync();
    }

    public void writeAndFlush(String message){
        clientChannel.writeAndFlush(new TextWebSocketFrame(message));
    }
    public void writeAndFlush(Object message){
        clientChannel.writeAndFlush(message);
    }

    public void ping(){
        clientChannel.writeAndFlush(new PingWebSocketFrame(Unpooled.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));
    }

    public void close() throws InterruptedException {
        clientChannel.writeAndFlush(new CloseWebSocketFrame());

        // WebSocketClientHandler will close the connection when the server
        // responds to the CloseWebSocketFrame.
        clientChannel.closeFuture().sync();
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        URI uri;
        if (args.length > 0) {
            uri = new URI(args[0]);
        } else {
            uri = new URI("http://192.168.11.1:8181/opendaylight-inventory:nodes");
        }
        IClientMessageCallback messageCallback = new ClientMessageCallback();
        WebSocketClient webSocketClient = new WebSocketClient(uri, messageCallback);
        webSocketClient.connect();
    }

    private static class ClientMessageCallback implements IClientMessageCallback {
        @Override
        public void onMessageReceived(Object message) {
            logger.info("received message {}", ((TextWebSocketFrame)message).text());
        }
    }

}
