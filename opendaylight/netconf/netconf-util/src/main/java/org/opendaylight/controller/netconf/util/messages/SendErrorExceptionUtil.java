/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public final class SendErrorExceptionUtil {
    private static final Logger logger = LoggerFactory.getLogger(SendErrorExceptionUtil.class);

    private SendErrorExceptionUtil() {}

    public static void sendErrorMessage(final NetconfSession session,
            final NetconfDocumentedException sendErrorException) {
        logger.trace("Sending error {}", sendErrorException.getMessage(), sendErrorException);
        final Document errorDocument = createDocument(sendErrorException);
        ChannelFuture f = session.sendMessage(new NetconfMessage(errorDocument));
        f.addListener(new SendErrorVerifyingListener(sendErrorException));
    }

    public static void sendErrorMessage(final Channel channel, final NetconfDocumentedException sendErrorException) {
        logger.trace("Sending error {}", sendErrorException.getMessage(), sendErrorException);
        final Document errorDocument = createDocument(sendErrorException);
        ChannelFuture f = channel.writeAndFlush(new NetconfMessage(errorDocument));
        f.addListener(new SendErrorVerifyingListener(sendErrorException));
    }

    public static void sendErrorMessage(final NetconfSession session, final NetconfDocumentedException sendErrorException,
            final NetconfMessage incommingMessage) {
        final Document errorDocument = createDocument(sendErrorException);

        if (logger.isTraceEnabled()) {
            logger.trace("Sending error {}", XmlUtil.toString(errorDocument));
        }
        tryToCopyAttributes(incommingMessage.getDocument(), errorDocument, sendErrorException);
        ChannelFuture f = session.sendMessage(new NetconfMessage(errorDocument));
        f.addListener(new SendErrorVerifyingListener(sendErrorException));
    }

    private static void tryToCopyAttributes(final Document incommingDocument, final Document errorDocument,
            final NetconfDocumentedException sendErrorException) {
        try {
            final Element incommingRpc = incommingDocument.getDocumentElement();
            Preconditions.checkState(incommingRpc.getTagName().equals(XmlNetconfConstants.RPC_KEY), "Missing %s element",
                    XmlNetconfConstants.RPC_KEY);

            final Element rpcReply = errorDocument.getDocumentElement();
            Preconditions.checkState(rpcReply.getTagName().equals(XmlNetconfConstants.RPC_REPLY_KEY), "Missing %s element",
                    XmlNetconfConstants.RPC_REPLY_KEY);

            final NamedNodeMap incomingAttributes = incommingRpc.getAttributes();
            for (int i = 0; i < incomingAttributes.getLength(); i++) {
                final Attr attr = (Attr) incomingAttributes.item(i);
                // skip namespace
                if (attr.getNodeName().equals(XmlUtil.XMLNS_ATTRIBUTE_KEY)) {
                    continue;
                }
                rpcReply.setAttributeNode((Attr) errorDocument.importNode(attr, true));
            }
        } catch (final Exception e) {
            logger.warn("Unable to copy incomming attributes to {}, returned rpc-error might be invalid for client",
                    sendErrorException, e);
        }
    }

    private static Document createDocument(final NetconfDocumentedException sendErrorException) {
        return sendErrorException.toXMLDocument();
    }

    /**
     * Checks if netconf error was sent successfully.
     */
    private static final class SendErrorVerifyingListener implements ChannelFutureListener {
        private final NetconfDocumentedException sendErrorException;

        public SendErrorVerifyingListener(final NetconfDocumentedException sendErrorException) {
            this.sendErrorException = sendErrorException;
        }

        @Override
        public void operationComplete(final ChannelFuture channelFuture) throws Exception {
            Preconditions.checkState(channelFuture.isSuccess(), "Unable to send exception %s", sendErrorException,
                    channelFuture.cause());
        }
    }
}
