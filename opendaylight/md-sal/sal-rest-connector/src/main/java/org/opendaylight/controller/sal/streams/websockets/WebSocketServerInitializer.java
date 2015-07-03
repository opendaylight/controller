package org.opendaylight.controller.sal.streams.websockets;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * {@link WebSocketServerInitializer} is used to setup the {@link ChannelPipeline} of a {@link io.netty.channel.Channel}
 * .
 */
public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final int MAX_CONTENT_LENGTH = 65536;

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("codec-http", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        pipeline.addLast("handler", new WebSocketServerHandler());
    }

}
