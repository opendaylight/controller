/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelInboundHandlerAdapter;
import org.opendaylight.controller.netconf.api.AbstractNetconfSession;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfSessionPreferences;
import org.opendaylight.controller.netconf.util.handler.FramingMechanismHandlerFactory;
import org.opendaylight.controller.netconf.util.handler.NetconfChunkAggregator;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToXMLEncoder;
import org.opendaylight.controller.netconf.util.handler.NetconfXMLToMessageDecoder;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public abstract class AbstractNetconfSessionNegotiator<P extends NetconfSessionPreferences, S extends AbstractNetconfSession<S, L>, L extends NetconfSessionListener<S>>
extends AbstractSessionNegotiator<NetconfHelloMessage, S> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNetconfSessionNegotiator.class);
    public static final String NAME_OF_EXCEPTION_HANDLER = "lastExceptionHandler";

    private final P sessionPreferences;

    private final L sessionListener;
    private Timeout timeout;

    /**
     * Possible states for Finite State Machine
     */
    private enum State {
        IDLE, OPEN_WAIT, FAILED, ESTABLISHED
    }

    private State state = State.IDLE;
    private final Timer timer;
    private final long connectionTimeoutMillis;

    protected AbstractNetconfSessionNegotiator(P sessionPreferences, Promise<S> promise, Channel channel, Timer timer,
            L sessionListener, long connectionTimeoutMillis) {
        super(promise, channel);
        this.sessionPreferences = sessionPreferences;
        this.timer = timer;
        this.sessionListener = sessionListener;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    @Override
    protected void startNegotiation() {
        final Optional<SslHandler> sslHandler = getSslHandler(channel);
        if (sslHandler.isPresent()) {
            Future<Channel> future = sslHandler.get().handshakeFuture();
            future.addListener(new GenericFutureListener<Future<? super Channel>>() {
                @Override
                public void operationComplete(Future<? super Channel> future) {
                    Preconditions.checkState(future.isSuccess(), "Ssl handshake was not successful");
                    logger.debug("Ssl handshake complete");
                    start();
                }
            });
        } else {
            start();
        }
    }

    private static Optional<SslHandler> getSslHandler(Channel channel) {
        final SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
        return sslHandler == null ? Optional.<SslHandler> absent() : Optional.of(sslHandler);
    }

    public P getSessionPreferences() {
        return sessionPreferences;
    }

    private void start() {
        final NetconfMessage helloMessage = this.sessionPreferences.getHelloMessage();
        logger.debug("Session negotiation started with hello message {}", XmlUtil.toString(helloMessage.getDocument()));

        channel.pipeline().addLast(NAME_OF_EXCEPTION_HANDLER, new ExceptionHandlingInboundChannelHandler());

        timeout = this.timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) {
                synchronized (this) {
                    if (state != State.ESTABLISHED) {
                        logger.debug("Connection timeout after {}, session is in state {}", timeout, state);
                        final IllegalStateException cause = new IllegalStateException(
                                "Session was not established after " + timeout);
                        negotiationFailed(cause);
                        changeState(State.FAILED);
                    } else if(channel.isOpen()) {
                        channel.pipeline().remove(NAME_OF_EXCEPTION_HANDLER);
                    }
                }
            }
        }, connectionTimeoutMillis, TimeUnit.MILLISECONDS);

        // FIXME, make sessionPreferences return HelloMessage, move NetconfHelloMessage to API
        sendMessage((NetconfHelloMessage)helloMessage);
        changeState(State.OPEN_WAIT);
    }

    private void cancelTimeout() {
        if(timeout!=null) {
            timeout.cancel();
        }
    }

    @Override
    protected void handleMessage(NetconfHelloMessage netconfMessage) {
        Preconditions.checkNotNull(netconfMessage != null, "netconfMessage");

        final Document doc = netconfMessage.getDocument();

        replaceHelloMessageHandlers();

        if (shouldUseChunkFraming(doc)) {
            insertChunkFramingToPipeline();
        }

        changeState(State.ESTABLISHED);
        S session = getSession(sessionListener, channel, netconfMessage);

        negotiationSuccessful(session);
    }

    /**
     * Insert chunk framing handlers into the pipeline
     */
    private void insertChunkFramingToPipeline() {
        replaceChannelHandler(channel, AbstractChannelInitializer.NETCONF_MESSAGE_FRAME_ENCODER,
                FramingMechanismHandlerFactory.createHandler(FramingMechanism.CHUNK));
        replaceChannelHandler(channel, AbstractChannelInitializer.NETCONF_MESSAGE_AGGREGATOR,
                new NetconfChunkAggregator());
    }

    private boolean shouldUseChunkFraming(Document doc) {
        return containsBase11Capability(doc)
                && containsBase11Capability(sessionPreferences.getHelloMessage().getDocument());
    }

    /**
     * Remove special handlers for hello message. Insert regular netconf xml message (en|de)coders.
     */
    private void replaceHelloMessageHandlers() {
        replaceChannelHandler(channel, AbstractChannelInitializer.NETCONF_MESSAGE_DECODER, new NetconfXMLToMessageDecoder());
        replaceChannelHandler(channel, AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, new NetconfMessageToXMLEncoder());
    }

    private static ChannelHandler replaceChannelHandler(Channel channel, String handlerKey, ChannelHandler decoder) {
        return channel.pipeline().replace(handlerKey, handlerKey, decoder);
    }

    protected abstract S getSession(L sessionListener, Channel channel, NetconfHelloMessage message);

    private synchronized void changeState(final State newState) {
        logger.debug("Changing state from : {} to : {}", state, newState);
        Preconditions.checkState(isStateChangePermitted(state, newState), "Cannot change state from %s to %s", state,
                newState);
        this.state = newState;
    }

    private boolean containsBase11Capability(final Document doc) {
        final NodeList nList = doc.getElementsByTagName("capability");
        for (int i = 0; i < nList.getLength(); i++) {
            if (nList.item(i).getTextContent().contains("base:1.1")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStateChangePermitted(State state, State newState) {
        if (state == State.IDLE && newState == State.OPEN_WAIT) {
            return true;
        }
        if (state == State.OPEN_WAIT && newState == State.ESTABLISHED) {
            return true;
        }
        if (state == State.OPEN_WAIT && newState == State.FAILED) {
            return true;
        }

        logger.debug("Transition from {} to {} is not allowed", state, newState);
        return false;
    }

    /**
     * Handler to catch exceptions in pipeline during negotiation
     */
    private final class ExceptionHandlingInboundChannelHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("An exception occurred during negotiation on channel {}", channel.localAddress(), cause);
            cancelTimeout();
            negotiationFailed(cause);
            changeState(State.FAILED);
        }
    }
}
