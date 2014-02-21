package org.opendaylight.controller.sal.streams.websockets;

import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * {@link WebSocketServer} is responsible to start and stop web socket server at
 * {@link #PORT}.
 */
public class WebSocketServer implements Runnable {

	private static final Logger logger = LoggerFactory
			.getLogger(WebSocketServer.class);

	public static final int PORT = 8181;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	@Override
	public void run() {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new WebSocketServerInitializer());

			Channel ch = b.bind(PORT).sync().channel();
			logger.info("Web socket server started at port {}.", PORT);

			ch.closeFuture().sync();
		} catch (InterruptedException e) {
			// NOOP
		} finally {
			stop();
		}
	}

	/**
	 * Stops the web socket server and removes all listeners.
	 */
	private void stop() {
		Notificator.removeAllListeners();
		if (bossGroup != null) {
			bossGroup.shutdownGracefully();
		}
		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
		}
	}

}
