/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.opendaylight.controller.netconf.api.NetconfClientSessionPreferences;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.xml.XMLNetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfClientSessionNegotiator extends
        AbstractNetconfSessionNegotiator<NetconfClientSessionPreferences, NetconfClientSession, NetconfClientSessionListener>
{
    private static final Logger logger = LoggerFactory.getLogger(NetconfClientSessionNegotiator.class);

    private static final XPathExpression sessionIdXPath = XMLNetconfUtil
            .compileXPath("/netconf:hello/netconf:session-id");

    private static final String EXI_1_0_CAPABILITY_MARKER = "exi:1.0";

    protected NetconfClientSessionNegotiator(NetconfClientSessionPreferences sessionPreferences,
                                             Promise<NetconfClientSession> promise,
                                             Channel channel,
                                             Timer timer,
                                             NetconfClientSessionListener sessionListener,
                                             long connectionTimeoutMillis) {
        super(sessionPreferences, promise, channel, timer, sessionListener, connectionTimeoutMillis);
    }

    @Override
    protected void handleMessage(NetconfHelloMessage netconfMessage) {
        NetconfClientSession session = super.getSessionForHelloMessage(netconfMessage);

        if (shouldUseExi(netconfMessage.getDocument())){
            logger.info("Netconf session: {} should use exi.", session);
            tryToStartExi(session);
        } else {
            logger.info("Netconf session {} isn't capable using exi.", session);
            negotiationSuccessful(session);
        }
    }

    private boolean shouldUseExi(Document doc) {
        return containsExi10Capability(doc)
                && containsExi10Capability(sessionPreferences.getHelloMessage().getDocument());
    }

    private boolean containsExi10Capability(final Document doc) {
        final NodeList nList = doc.getElementsByTagName(XmlNetconfConstants.CAPABILITY);
        for (int i = 0; i < nList.getLength(); i++) {
            if (nList.item(i).getTextContent().contains(EXI_1_0_CAPABILITY_MARKER)) {
                return true;
            }
        }
        return false;
    }

    private void tryToStartExi(final NetconfClientSession session) {
        final NetconfMessage startExi = sessionPreferences.getStartExiMessage();
        session.sendMessage(startExi).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture f) {
                if (!f.isSuccess()) {
                    logger.warn("Failed to send start-exi message {} on session {}", startExi, session, f.cause());
                } else {
                    logger.trace("Start-exi message {} sent to socket on session {}", startExi, session);
                    NetconfClientSessionNegotiator.this.channel.pipeline().addAfter(
                            AbstractChannelInitializer.NETCONF_MESSAGE_DECODER, ExiConfirmationInboundHandler.EXI_CONFIRMED_HANDLER,
                            new ExiConfirmationInboundHandler(session));
                }
            }
        });
    }

    private long extractSessionId(Document doc) {
        final Node sessionIdNode = (Node) XmlUtil.evaluateXPath(sessionIdXPath, doc, XPathConstants.NODE);
        String textContent = sessionIdNode.getTextContent();
        if (textContent == null || textContent.equals("")) {
            throw new IllegalStateException("Session id not received from server");
        }

        return Long.valueOf(textContent);
    }

    @Override
    protected NetconfClientSession getSession(NetconfClientSessionListener sessionListener, Channel channel, NetconfHelloMessage message) {
        return new NetconfClientSession(sessionListener, channel, extractSessionId(message.getDocument()),
                NetconfMessageUtil.extractCapabilitiesFromHello(message.getDocument()));
    }

    /**
     * Handler to process response for start-exi message
     */
    private final class ExiConfirmationInboundHandler extends ChannelInboundHandlerAdapter {
        private static final String EXI_CONFIRMED_HANDLER = "exiConfirmedHandler";

        private final NetconfClientSession session;

        ExiConfirmationInboundHandler(NetconfClientSession session) {
            this.session = session;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.pipeline().remove(ExiConfirmationInboundHandler.EXI_CONFIRMED_HANDLER);

            NetconfMessage netconfMessage = (NetconfMessage) msg;

            // Ok response to start-exi, try to add exi handlers
            if (NetconfMessageUtil.isOKMessage(netconfMessage)) {
                logger.trace("Positive response on start-exi call received on session {}", session);
                try {
                    session.startExiCommunication(sessionPreferences.getStartExiMessage());
                } catch (RuntimeException e) {
                    // Unable to add exi, continue without exi
                    logger.warn("Unable to start exi communication, Communication will continue without exi on session {}", session, e);
                }

            // Error response
            } else if(NetconfMessageUtil.isErrorMessage(netconfMessage)) {
                logger.warn(
                        "Error response to start-exi message {}, Communication will continue without exi on session {}",
                        XmlUtil.toString(netconfMessage.getDocument()), session);

            // Unexpected response to start-exi, throwing message away, continue without exi
            } else {
                logger.warn(
                        "Unexpected response to start-exi message, should be ok, was {}, " +
                                "Communication will continue without exi and response message will be thrown away on session {}",
                        XmlUtil.toString(netconfMessage.getDocument()), session);
            }

            negotiationSuccessful(session);
        }
    }
}
