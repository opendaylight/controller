/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Collection;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.opendaylight.controller.netconf.api.NetconfClientSessionPreferences;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.messages.NetconfStartExiMessage;
import org.opendaylight.controller.netconf.util.xml.XMLNetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.netty.channel.Channel;
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
    protected void handleMessage(NetconfHelloMessage netconfMessage) throws NetconfDocumentedException {
        final NetconfClientSession session = getSessionForHelloMessage(netconfMessage);
        replaceHelloMessageInboundHandler(session);

        // If exi should be used, try to initiate exi communication
        // Call negotiationSuccessFul after exi negotiation is finished successfully or not
        if (shouldUseExi(netconfMessage)) {
            logger.debug("Netconf session {} should use exi.", session);
            NetconfStartExiMessage startExiMessage = (NetconfStartExiMessage) sessionPreferences.getStartExiMessage();
            tryToInitiateExi(session, startExiMessage);
        // Exi is not supported, release session immediately
        } else {
            logger.debug("Netconf session {} isn't capable of using exi.", session);
            negotiationSuccessful(session);
        }
    }

    /**
     * Initiates exi communication by sending start-exi message and waiting for positive/negative response.
     *
     * @param startExiMessage
     */
    void tryToInitiateExi(final NetconfClientSession session, final NetconfStartExiMessage startExiMessage) {
        channel.pipeline().addAfter(AbstractChannelInitializer.NETCONF_MESSAGE_DECODER,
                ExiConfirmationInboundHandler.EXI_CONFIRMED_HANDLER,
                new ExiConfirmationInboundHandler(session, startExiMessage));

        session.sendMessage(startExiMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture f) {
                if (!f.isSuccess()) {
                    logger.warn("Failed to send start-exi message {} on session {}", startExiMessage, this, f.cause());
                    channel.pipeline().remove(ExiConfirmationInboundHandler.EXI_CONFIRMED_HANDLER);
                } else {
                    logger.trace("Start-exi message {} sent to socket on session {}", startExiMessage, this);
                }
            }
        });
    }

    private boolean shouldUseExi(NetconfHelloMessage helloMsg) {
        return containsExi10Capability(helloMsg.getDocument())
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

    private long extractSessionId(Document doc) {
        final Node sessionIdNode = (Node) XmlUtil.evaluateXPath(sessionIdXPath, doc, XPathConstants.NODE);
        String textContent = sessionIdNode.getTextContent();
        if (textContent == null || textContent.equals("")) {
            throw new IllegalStateException("Session id not received from server");
        }

        return Long.valueOf(textContent);
    }

    @Override
    protected NetconfClientSession getSession(NetconfClientSessionListener sessionListener, Channel channel,
            NetconfHelloMessage message) throws NetconfDocumentedException {
        long sessionId = extractSessionId(message.getDocument());
        Collection<String> capabilities = NetconfMessageUtil.extractCapabilitiesFromHello(message.getDocument());
        return new NetconfClientSession(sessionListener, channel, sessionId, capabilities);
    }

    /**
     * Handler to process response for start-exi message
     */
    private final class ExiConfirmationInboundHandler extends ChannelInboundHandlerAdapter {
        private static final String EXI_CONFIRMED_HANDLER = "exiConfirmedHandler";

        private final NetconfClientSession session;
        private NetconfStartExiMessage startExiMessage;

        ExiConfirmationInboundHandler(NetconfClientSession session, final NetconfStartExiMessage startExiMessage) {
            this.session = session;
            this.startExiMessage = startExiMessage;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.pipeline().remove(ExiConfirmationInboundHandler.EXI_CONFIRMED_HANDLER);

            NetconfMessage netconfMessage = (NetconfMessage) msg;

            // Ok response to start-exi, try to add exi handlers
            if (NetconfMessageUtil.isOKMessage(netconfMessage)) {
                logger.trace("Positive response on start-exi call received on session {}", session);
                try {
                    session.startExiCommunication(startExiMessage);
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
