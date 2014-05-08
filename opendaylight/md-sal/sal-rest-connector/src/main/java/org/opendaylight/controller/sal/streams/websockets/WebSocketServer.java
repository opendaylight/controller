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
public class WebSocketServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    public static final String WEBSOCKET_SERVER_CONFIG_PROPERTY = "restconf.websocket.port";
    public static final int DEFAULT_PORT = 8181;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private static WebSocketServer singleton = null;
    private int port = DEFAULT_PORT;

    private WebSocketServer(int port) {
        this.port = port;
    }

    /**
     * Create instance of {@link WebSocketServer}
     *
     * @param port
     *            TCP port used for this server
     * @return instance of {@link WebSocketServer}
     */
    public static WebSocketServer createInstance(int port) {
        if (singleton != null) {
            throw new IllegalStateException("createInstance() has already been called");
        }
        if (port < 1024) {
            throw new IllegalArgumentException("Privileged port (below 1024) is not allowed");
        }
        singleton = new WebSocketServer(port);
        return singleton;
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
        Preconditions.checkNotNull(singleton, "createInstance() must be called prior to getInstance()");
        return singleton;
    }

    /**
     * Destroy this already created instance
     */
    public static void destroyInstance() {
        if (singleton == null) {
            throw new IllegalStateException("createInstance() must be called prior to destroyInstance()");
        }
        getInstance().stop();
    }

    @Override
    public void run() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketServerInitializer());

            Channel ch = b.bind(port).sync().channel();
            logger.info("Web socket server started at port {}.", port);

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
