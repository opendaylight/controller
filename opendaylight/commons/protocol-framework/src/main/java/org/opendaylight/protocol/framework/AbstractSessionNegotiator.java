/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Abstract base class for session negotiators. It implements the basic
 * substrate to implement SessionNegotiator API specification, with subclasses
 * needing to provide only
 *
 * @param <M> Protocol message type
 * @param <S> Protocol session type, has to extend {@code ProtocolSession<M>}
 */
@Deprecated
public abstract class AbstractSessionNegotiator<M, S extends AbstractProtocolSession<?>> extends ChannelInboundHandlerAdapter implements SessionNegotiator<S> {
    private final Logger LOG = LoggerFactory.getLogger(AbstractSessionNegotiator.class);
    private final Promise<S> promise;
    protected final Channel channel;

    public AbstractSessionNegotiator(final Promise<S> promise, final Channel channel) {
        this.promise = Preconditions.checkNotNull(promise);
        this.channel = Preconditions.checkNotNull(channel);
    }

    protected abstract void startNegotiation() throws Exception;
    protected abstract void handleMessage(M msg) throws Exception;

    protected final void negotiationSuccessful(final S session) {
        LOG.debug("Negotiation on channel {} successful with session {}", channel, session);
        channel.pipeline().replace(this, "session", session);
        promise.setSuccess(session);
    }

    protected void negotiationFailed(final Throwable cause) {
        LOG.debug("Negotiation on channel {} failed", channel, cause);
        channel.close();
        promise.setFailure(cause);
    }

    /**
     * Send a message to peer and fail negotiation if it does not reach
     * the peer.
     *
     * @param msg Message which should be sent.
     */
    protected final void sendMessage(final M msg) {
        this.channel.writeAndFlush(msg).addListener(
                (ChannelFutureListener) f -> {
                    if (!f.isSuccess()) {
                        LOG.info("Failed to send message {}", msg, f.cause());
                        negotiationFailed(f.cause());
                    } else {
                        LOG.trace("Message {} sent to socket", msg);
                    }
                });
    }

    @Override
    public final void channelActive(final ChannelHandlerContext ctx) {
        LOG.debug("Starting session negotiation on channel {}", channel);
        try {
            startNegotiation();
        } catch (final Exception e) {
            LOG.warn("Unexpected negotiation failure", e);
            negotiationFailed(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        LOG.debug("Negotiation read invoked on channel {}", channel);
        try {
            handleMessage((M)msg);
        } catch (final Exception e) {
            LOG.debug("Unexpected error while handling negotiation message {}", msg, e);
            negotiationFailed(e);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.info("Unexpected error during negotiation", cause);
        negotiationFailed(cause);
    }
}
