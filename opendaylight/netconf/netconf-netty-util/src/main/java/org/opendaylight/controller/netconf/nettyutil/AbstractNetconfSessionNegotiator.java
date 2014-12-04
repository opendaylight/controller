/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfSessionPreferences;
import org.opendaylight.controller.netconf.nettyutil.handler.FramingMechanismHandlerFactory;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfChunkAggregator;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfMessageToXMLEncoder;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfXMLToHelloMessageDecoder;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfXMLToMessageDecoder;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.protocol.framework.AbstractSessionNegotiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public abstract class AbstractNetconfSessionNegotiator<P extends NetconfSessionPreferences, S extends AbstractNetconfSession<S, L>, L extends NetconfSessionListener<S>>
    extends AbstractSessionNegotiator<NetconfHelloMessage, S> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetconfSessionNegotiator.class);

    public static final String NAME_OF_EXCEPTION_HANDLER = "lastExceptionHandler";

    protected final P sessionPreferences;

    private final L sessionListener;
    private Timeout timeout;

    /**
     * Possible states for Finite State Machine
     */
    protected enum State {
        IDLE, OPEN_WAIT, FAILED, ESTABLISHED
    }

    private State state = State.IDLE;
    private final Promise<S> promise;
    private final Timer timer;
    private final long connectionTimeoutMillis;

    // TODO shrink constructor
    protected AbstractNetconfSessionNegotiator(final P sessionPreferences, final Promise<S> promise, final Channel channel, final Timer timer,
            final L sessionListener, final long connectionTimeoutMillis) {
        super(promise, channel);
        this.sessionPreferences = sessionPreferences;
        this.promise = promise;
        this.timer = timer;
        this.sessionListener = sessionListener;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    @Override
    protected final void startNegotiation() {
        final Optional<SslHandler> sslHandler = getSslHandler(channel);
        if (sslHandler.isPresent()) {
            Future<Channel> future = sslHandler.get().handshakeFuture();
            future.addListener(new GenericFutureListener<Future<? super Channel>>() {
                @Override
                public void operationComplete(final Future<? super Channel> future) {
                    Preconditions.checkState(future.isSuccess(), "Ssl handshake was not successful");
                    LOG.debug("Ssl handshake complete");
                    start();
                }
            });
        } else {
            start();
        }
    }

    private static Optional<SslHandler> getSslHandler(final Channel channel) {
        final SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
        return sslHandler == null ? Optional.<SslHandler> absent() : Optional.of(sslHandler);
    }

    public P getSessionPreferences() {
        return sessionPreferences;
    }

    private void start() {
        final NetconfMessage helloMessage = this.sessionPreferences.getHelloMessage();
        LOG.debug("Session negotiation started with hello message {} on channel {}", helloMessage, channel);

        channel.pipeline().addLast(NAME_OF_EXCEPTION_HANDLER, new ExceptionHandlingInboundChannelHandler());

        // FIXME, make sessionPreferences return HelloMessage, move NetconfHelloMessage to API
        sendMessage((NetconfHelloMessage)helloMessage);

        replaceHelloMessageOutboundHandler();
        changeState(State.OPEN_WAIT);

        timeout = this.timer.newTimeout(new TimerTask() {
            @Override
            public void run(final Timeout timeout) {
                synchronized (this) {
                    if (state != State.ESTABLISHED) {

                        LOG.debug("Connection timeout after {}, session is in state {}", timeout, state);

                        // Do not fail negotiation if promise is done or canceled
                        // It would result in setting result of the promise second time and that throws exception
                        if (isPromiseFinished() == false) {
                            negotiationFailed(new IllegalStateException("Session was not established after " + timeout));
                            changeState(State.FAILED);

                            channel.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
                                @Override
                                public void operationComplete(final ChannelFuture future) throws Exception {
                                    if(future.isSuccess()) {
                                        LOG.debug("Channel {} closed: success", future.channel());
                                    } else {
                                        LOG.warn("Channel {} closed: fail", future.channel());
                                    }
                                }
                            });
                        }
                    } else if(channel.isOpen()) {
                        channel.pipeline().remove(NAME_OF_EXCEPTION_HANDLER);
                    }
                }
            }

            private boolean isPromiseFinished() {
                return promise.isDone() || promise.isCancelled();
            }

        }, connectionTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelTimeout() {
        if(timeout!=null) {
            timeout.cancel();
        }
    }

    protected final S getSessionForHelloMessage(final NetconfHelloMessage netconfMessage) throws NetconfDocumentedException {
        Preconditions.checkNotNull(netconfMessage, "netconfMessage");

        final Document doc = netconfMessage.getDocument();

        if (shouldUseChunkFraming(doc)) {
            insertChunkFramingToPipeline();
        }

        changeState(State.ESTABLISHED);
        return getSession(sessionListener, channel, netconfMessage);
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

    private boolean shouldUseChunkFraming(final Document doc) {
        return containsBase11Capability(doc)
                && containsBase11Capability(sessionPreferences.getHelloMessage().getDocument());
    }

    /**
     * Remove special inbound handler for hello message. Insert regular netconf xml message (en|de)coders.
     *
     * Inbound hello message handler should be kept until negotiation is successful
     * It caches any non-hello messages while negotiation is still in progress
     */
    protected final void replaceHelloMessageInboundHandler(final S session) {
        ChannelHandler helloMessageHandler = replaceChannelHandler(channel, AbstractChannelInitializer.NETCONF_MESSAGE_DECODER, new NetconfXMLToMessageDecoder());

        Preconditions.checkState(helloMessageHandler instanceof NetconfXMLToHelloMessageDecoder,
                "Pipeline handlers misplaced on session: %s, pipeline: %s", session, channel.pipeline());
        Iterable<NetconfMessage> netconfMessagesFromNegotiation =
                ((NetconfXMLToHelloMessageDecoder) helloMessageHandler).getPostHelloNetconfMessages();

        // Process messages received during negotiation
        // The hello message handler does not have to be synchronized, since it is always call from the same thread by netty
        // It means, we are now using the thread now
        for (NetconfMessage message : netconfMessagesFromNegotiation) {
            session.handleMessage(message);
        }
    }

    /**
     * Remove special outbound handler for hello message. Insert regular netconf xml message (en|de)coders.
     */
    private void replaceHelloMessageOutboundHandler() {
        replaceChannelHandler(channel, AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, new NetconfMessageToXMLEncoder());
    }

    private static ChannelHandler replaceChannelHandler(final Channel channel, final String handlerKey, final ChannelHandler decoder) {
        return channel.pipeline().replace(handlerKey, handlerKey, decoder);
    }

    protected abstract S getSession(L sessionListener, Channel channel, NetconfHelloMessage message) throws NetconfDocumentedException;

    private synchronized void changeState(final State newState) {
        LOG.debug("Changing state from : {} to : {} for channel: {}", state, newState, channel);
        Preconditions.checkState(isStateChangePermitted(state, newState), "Cannot change state from %s to %s for chanel %s", state,
                newState, channel);
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

    private static boolean isStateChangePermitted(final State state, final State newState) {
        if (state == State.IDLE && newState == State.OPEN_WAIT) {
            return true;
        }
        if (state == State.OPEN_WAIT && newState == State.ESTABLISHED) {
            return true;
        }
        if (state == State.OPEN_WAIT && newState == State.FAILED) {
            return true;
        }
        LOG.debug("Transition from {} to {} is not allowed", state, newState);
        return false;
    }

    /**
     * Handler to catch exceptions in pipeline during negotiation
     */
    private final class ExceptionHandlingInboundChannelHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            LOG.warn("An exception occurred during negotiation with {}", channel.remoteAddress(), cause);
            cancelTimeout();
            negotiationFailed(cause);
            changeState(State.FAILED);
        }
    }
}
