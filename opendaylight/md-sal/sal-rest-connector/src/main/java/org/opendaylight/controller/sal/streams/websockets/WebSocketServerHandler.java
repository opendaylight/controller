package org.opendaylight.controller.sal.streams.websockets;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WebSocketServerHandler} is implementation of {@link SimpleChannelInboundHandler} which allow handle
 * {@link FullHttpRequest} and {@link WebSocketFrame} messages.
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * Checks if HTTP request method is GET and if is possible to decode HTTP result of request.
     *
     * @param ctx
     *            ChannelHandlerContext
     * @param req
     *            FullHttpRequest
     */
    private void handleHttpRequest(final ChannelHandlerContext ctx, final FullHttpRequest req) throws Exception {
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.getMethod() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        String streamName = Notificator.createStreamNameFromUri(req.getUri());
        ListenerAdapter listener = Notificator.getListenerFor(streamName);
        if (listener != null) {
            listener.addSubscriber(ctx.channel());
            logger.debug("Subscriber successfully registered.");
        } else {
            logger.error("Listener for stream with name '{}' was not found.", streamName);
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR));
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req),
                null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }

    }

    /**
     * Checks response status, send response and close connection if necessary
     *
     * @param ctx
     *            ChannelHandlerContext
     * @param req
     *            HttpRequest
     * @param res
     *            FullHttpResponse
     */
    private static void sendHttpResponse(final ChannelHandlerContext ctx, final HttpRequest req,
            final FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Handles web socket frame.
     *
     * @param ctx
     *            {@link ChannelHandlerContext}
     * @param frame
     *            {@link WebSocketFrame}
     */
    private void handleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame frame) throws IOException {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            String streamName = Notificator.createStreamNameFromUri(((CloseWebSocketFrame) frame).reasonText());
            ListenerAdapter listener = Notificator.getListenerFor(streamName);
            if (listener != null) {
                listener.removeSubscriber(ctx.channel());
                logger.debug("Subscriber successfully registered.");
            }
            Notificator.removeListenerIfNoSubscriberExists(listener);
            return;
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause instanceof java.nio.channels.ClosedChannelException == false) {
            // cause.printStackTrace();
        }
        ctx.close();
    }

    /**
     * Get web socket location from HTTP request.
     *
     * @param req
     *            HTTP request from which the location will be returned
     * @return String representation of web socket location.
     */
    private static String getWebSocketLocation(final HttpRequest req) {
        return "ws://" + req.headers().get(HOST) + req.getUri();
    }

}
