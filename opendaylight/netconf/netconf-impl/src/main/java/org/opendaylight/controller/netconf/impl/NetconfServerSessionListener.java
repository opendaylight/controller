/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;


import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationRouter;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.impl.mapping.operations.DefaultCloseSession;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.util.messages.SendErrorExceptionUtil;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class NetconfServerSessionListener implements NetconfSessionListener<NetconfServerSession> {

    static final Logger logger = LoggerFactory.getLogger(NetconfServerSessionListener.class);
    private final SessionMonitoringService monitoringService;
    private final NetconfOperationRouter operationRouter;
    private final AutoCloseable onSessionDownCloseable;

    public NetconfServerSessionListener(NetconfOperationRouter operationRouter, SessionMonitoringService monitoringService,
                                        AutoCloseable onSessionDownCloseable) {
        this.operationRouter = operationRouter;
        this.monitoringService = monitoringService;
        this.onSessionDownCloseable = onSessionDownCloseable;
    }

    @Override
    public void onSessionUp(NetconfServerSession netconfNetconfServerSession) {
        monitoringService.onSessionUp(netconfNetconfServerSession);
    }

    @Override
    public void onSessionDown(NetconfServerSession netconfNetconfServerSession, Exception cause) {
        logger.debug("Session {} down, reason: {}", netconfNetconfServerSession, cause.getMessage());
        onDown(netconfNetconfServerSession);
    }

    public void onDown(NetconfServerSession netconfNetconfServerSession) {
        monitoringService.onSessionDown(netconfNetconfServerSession);

        try {
            operationRouter.close();
        } catch (Exception closingEx) {
            logger.debug("Ignoring exception while closing operationRouter", closingEx);
        }
        try {
            onSessionDownCloseable.close();
        } catch(Exception ex){
            logger.debug("Ignoring exception while closing onSessionDownCloseable", ex);
        }
    }

    @Override
    public void onSessionTerminated(NetconfServerSession netconfNetconfServerSession,
            NetconfTerminationReason netconfTerminationReason) {
        logger.debug("Session {} terminated, reason: {}", netconfNetconfServerSession,
                netconfTerminationReason.getErrorMessage());
        onDown(netconfNetconfServerSession);
    }

    @Override
    public void onMessage(NetconfServerSession session, NetconfMessage netconfMessage) {
        try {

            Preconditions.checkState(operationRouter != null, "Cannot handle message, session up was not yet received");
            // FIXME: there is no validation since the document may contain yang
            // schemas
            final NetconfMessage message = processDocument(netconfMessage,
                    session);
            logger.debug("Responding with message {}", XmlUtil.toString(message.getDocument()));
            session.sendMessage(message);

            if (isCloseSession(netconfMessage)) {
                closeNetconfSession(session);
            }

        } catch (final RuntimeException e) {
            // TODO: should send generic error or close session?
            logger.error("Unexpected exception", e);
            session.onIncommingRpcFail();
            throw new RuntimeException("Unable to process incoming message " + netconfMessage, e);
        } catch (NetconfDocumentedException e) {
            session.onOutgoingRpcError();
            session.onIncommingRpcFail();
            SendErrorExceptionUtil.sendErrorMessage(session, e, netconfMessage);
        }
    }

    private void closeNetconfSession(NetconfServerSession session) {
        // destroy NetconfOperationService
        session.close();
        logger.info("Session {} closed successfully", session.getSessionId());
    }

    private NetconfMessage processDocument(final NetconfMessage netconfMessage, NetconfServerSession session)
            throws NetconfDocumentedException {

        final Document incomingDocument = netconfMessage.getDocument();
        final Node rootNode = incomingDocument.getDocumentElement();

        if (rootNode.getLocalName().equals(XmlNetconfConstants.RPC_KEY)) {
            final Document responseDocument = XmlUtil.newDocument();
            checkMessageId(rootNode);

            Document rpcReply = operationRouter.onNetconfMessage(incomingDocument, session);

            session.onIncommingRpcSuccess();

            responseDocument.appendChild(responseDocument.importNode(rpcReply.getDocumentElement(), true));
            return new NetconfMessage(responseDocument);
        } else {
            // unknown command, send RFC 4741 p.70 unknown-element
            /*
             * Tag: unknown-element Error-type: rpc, protocol, application
             * Severity: error Error-info: <bad-element> : name of the
             * unexpected element Description: An unexpected element is present.
             */
            // TODO add message to error info
            throw new NetconfDocumentedException("Unknown tag " + rootNode.getNodeName(),
                    NetconfDocumentedException.ErrorType.protocol, NetconfDocumentedException.ErrorTag.unknown_element,
                    NetconfDocumentedException.ErrorSeverity.error, ImmutableMap.of("bad-element",
                            rootNode.getNodeName()));
        }
    }

    private void checkMessageId(Node rootNode) throws NetconfDocumentedException {
            NamedNodeMap attributes = rootNode.getAttributes();
        if(attributes.getNamedItemNS(XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, XmlNetconfConstants.MESSAGE_ID)!=null) {
            return;
        }

        if(attributes.getNamedItem(XmlNetconfConstants.MESSAGE_ID)!=null) {
            return;
        }

        throw new NetconfDocumentedException("Missing attribute" + rootNode.getNodeName(),
                NetconfDocumentedException.ErrorType.protocol, NetconfDocumentedException.ErrorTag.missing_attribute,
                NetconfDocumentedException.ErrorSeverity.error, ImmutableMap.of(NetconfDocumentedException.ErrorTag.missing_attribute.toString(),
                XmlNetconfConstants.MESSAGE_ID));
    }

    private static boolean isCloseSession(final NetconfMessage incomingDocument) {
        final Document document = incomingDocument.getDocument();
        XmlElement rpcElement = XmlElement.fromDomDocument(document);
        if (rpcElement.getOnlyChildElementOptionally(DefaultCloseSession.CLOSE_SESSION).isPresent()) {
            return true;
        }

        return false;
    }
}
