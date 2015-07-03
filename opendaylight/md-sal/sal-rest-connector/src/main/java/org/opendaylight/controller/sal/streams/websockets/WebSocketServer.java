package org.opendaylight.controller.sal.streams.websockets;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WebSocketServer} is responsible to start and stop web socket server
 */
public final class WebSocketServer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServer.class);
    public static final int DEFAULT_PORT = 8181;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private static WebSocketServer instance = null;
    private int port = DEFAULT_PORT;
    private static final int LOWER_LIMIT_PORT = 1024;

    private WebSocketServer(final int port) {
        this.port = port;
    }

    /**
     * Create instance of {@link WebSocketServer}
     *
     * @param port
     *            TCP port used for this server
     * @return instance of {@link WebSocketServer}
     */
    public static WebSocketServer createInstance(final int port) {
        Preconditions.checkState(instance == null, "createInstance() has already been called");
        Preconditions.checkArgument(port > LOWER_LIMIT_PORT, "Privileged port (below " + LOWER_LIMIT_PORT
                + ") is not allowed");

        instance = new WebSocketServer(port);
        return instance;
    }

    /**
     * Return websocket TCP port
     */
    public int getPort() {
        return port;
    }

    /**
     * Get instance of {@link WebSocketServer} created by {@link #createInstance(int)}
     *
     * @return instance of {@link WebSocketServer}
     */
    public static WebSocketServer getInstance() {
        Preconditions.checkState(instance != null, "createInstance() must be called prior to getInstance()");
        return instance;
    }

    /**
     * Destroy this already created instance
     */
    public static void destroyInstance() {
        Preconditions.checkState(instance != null, "createInstance() must be called prior to destroyInstance()");

        instance.stop();
        instance = null;
    }

    @Override
    public void run() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketServerInitializer());

            final Channel ch = b.bind(port).sync().channel();
            LOG.info("Web socket server started at port {}.", port);

            ch.closeFuture().sync();
        } catch (final InterruptedException e) {
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
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

}
