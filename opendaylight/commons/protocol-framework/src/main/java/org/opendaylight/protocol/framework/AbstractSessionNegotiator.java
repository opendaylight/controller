/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.Channel;
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
 * @param <S> Protocol session type, has to extend ProtocolSession<M>
 */
public abstract class AbstractSessionNegotiator<M, S extends AbstractProtocolSession<?>> extends ChannelInboundHandlerAdapter implements SessionNegotiator<S> {
    private final Logger logger = LoggerFactory.getLogger(AbstractSessionNegotiator.class);
    private final Promise<S> promise;
    protected final Channel channel;

    public AbstractSessionNegotiator(final Promise<S> promise, final Channel channel) {
        this.promise = Preconditions.checkNotNull(promise);
        this.channel = Preconditions.checkNotNull(channel);
    }

    protected abstract void startNegotiation() throws Exception;
    protected abstract void handleMessage(M msg) throws Exception;

    protected final void negotiationSuccessful(final S session) {
        logger.debug("Negotiation on channel {} successful with session {}", channel, session);
        channel.pipeline().replace(this, "session", session);
        promise.setSuccess(session);
    }

    protected final void negotiationFailed(final Throwable cause) {
        logger.debug("Negotiation on channel {} failed", channel, cause);
        channel.close();
        promise.setFailure(cause);
    }

    @Override
    public final void channelActive(final ChannelHandlerContext ctx) {
        logger.debug("Starting session negotiation on channel {}", channel);
        try {
            startNegotiation();
        } catch (Exception e) {
            logger.info("Unexpected negotiation failure", e);
            negotiationFailed(e);
        }
    }

    @Override
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        logger.debug("Negotiation read invoked on channel {}", channel);
        try {
            handleMessage((M)msg);
        } catch (Exception e) {
            logger.debug("Unexpected exception during negotiation", e);
            negotiationFailed(e);
        }
    }
}
