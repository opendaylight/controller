/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.AbstractNetconfSession;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfSessionPreferences;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.controller.netconf.util.handler.NetconfEXICodec;
import org.opendaylight.controller.netconf.util.handler.NetconfEXIToMessageDecoder;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageToEXIEncoder;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.xml.EXIParameters;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.openexi.proc.common.EXIOptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public abstract class AbstractNetconfSessionExiNegotiator<P extends NetconfSessionPreferences, S extends AbstractNetconfSession<S, L>, L extends NetconfSessionListener<S>>
        extends AbstractNetconfSessionNegotiator<P,S,L> {

    private final NetconfSessionPreferences sessionPreferences;
    private static final Logger logger = LoggerFactory.getLogger(AbstractNetconfSessionExiNegotiator.class);
    private final L sessionListener;
    private S session;

    protected AbstractNetconfSessionExiNegotiator(P sessionPreferences,Promise<S> promise, Channel channel, Timer timer, L sessionListener, long connectionTimeoutMillis) {
        super(sessionPreferences, promise, channel, timer, sessionListener, connectionTimeoutMillis);
        this.sessionPreferences = sessionPreferences;
        this.sessionListener = sessionListener;
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
        this.session = getSession(sessionListener, channel, netconfMessage);
        if (shouldUseExi(netconfMessage.getDocument())){
            logger.info("Netconf session should use exi.");
            sendStarExiMessage();
        } else {
            logger.info("Netconf session isn't capable using exi.");
            negotiationSuccessful(session);
        }
    }

    private boolean shouldUseExi(Document doc) {
        return containsExi10Capability(doc)
                && containsExi10Capability(sessionPreferences.getHelloMessage().getDocument());
    }
    private boolean containsExi10Capability(final Document doc) {
        final NodeList nList = doc.getElementsByTagName("capability");
        for (int i = 0; i < nList.getLength(); i++) {
            if (nList.item(i).getTextContent().contains("exi:1.0")) {
                return true;
            }
        }
        return false;
    }

    private void sendStarExiMessage(){
        final NetconfMessage startExi = sessionPreferences.getStartExiMessage();
        this.channel.pipeline().addAfter("netconfMessageDecoder","exiConfirmedHandler",new ExiConfirmationInboundHandler()).writeAndFlush(startExi).addListener(
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture f) {
                        if (!f.isSuccess()) {
                            logger.info("Failed to send message {}", startExi, f.cause());
                        } else {
                            logger.trace("Message {} sent to socket", startExi);
                        }
                    }
                });
    }
    private void addExiHandlers(){
        try {
            EXIParameters exiParams = EXIParameters.forXmlElement(XmlElement.fromDomDocument(sessionPreferences.getStartExiMessage().getDocument()));
            NetconfEXICodec exiCodec = new NetconfEXICodec(exiParams.getOptions());
            channel.pipeline().addBefore("netconfMessageEncoder", NetconfMessageToEXIEncoder.HANDLER_NAME,new NetconfMessageToEXIEncoder(exiCodec));
            channel.pipeline().addAfter("netconfMessageDecoder", NetconfEXIToMessageDecoder.HANDLER_NAME,new NetconfEXIToMessageDecoder(exiCodec));
            logger.info("EXI handlers added to pipeline.");
        } catch (EXIOptionsException e) {
            logger.debug("Failed to parse EXI parameters", e);
        }
    }
    /**
     * Handler to process response on start-exi message
     */
    private class ExiConfirmationInboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.pipeline().remove(ExiConfirmationInboundHandler.class);
            NetconfMessage responseMessage = (NetconfMessage)msg;
            responseMessage.getDocument().getDocumentElement();
            try {
                if (XmlElement.fromDomDocument(responseMessage.getDocument()).getOnlyChildElement().getName().equals("ok")){
                    logger.info("Positive response on start-exi call received.");
                    addExiHandlers();
                }
            } catch (Exception e){
                logger.info("Response on start-exi message did not contain \"OK\" exi encoding won't be used.");
            }
            negotiationSuccessful(session);
        }
    }


}
