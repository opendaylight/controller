/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.mapping.operations;

import java.io.InputStream;
import java.util.Map;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilterChain;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation.OperationNameAndNamespace;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Maps;

public class DefaultCommit implements NetconfOperationFilter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCommit.class);

    private static final String NOTIFY_ATTR = "notify";

    private final DefaultCommitNotificationProducer notificationProducer;
    private final CapabilityProvider cap;
    private final String netconfSessionIdForReporting;

    public DefaultCommit(DefaultCommitNotificationProducer notifier, CapabilityProvider cap,
            String netconfSessionIdForReporting) {
        this.notificationProducer = notifier;
        this.cap = cap;
        this.netconfSessionIdForReporting = netconfSessionIdForReporting;
        this.getConfigMessage = loadGetConfigMessage();
    }

    private final Document getConfigMessage;
    public static final String GET_CONFIG_CANDIDATE_XML_LOCATION = "/getConfig_candidate.xml";

    private static Document loadGetConfigMessage() {
        try (InputStream asStream = DefaultCommit.class.getResourceAsStream(GET_CONFIG_CANDIDATE_XML_LOCATION)) {
            return XmlUtil.readXmlToDocument(asStream);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load getConfig message for notifications from "
                    + GET_CONFIG_CANDIDATE_XML_LOCATION);
        }
    }

    @Override
    public Document doFilter(Document message, NetconfOperationRouter operationRouter,
            NetconfOperationFilterChain filterChain) throws NetconfDocumentedException {
        OperationNameAndNamespace operationNameAndNamespace = new OperationNameAndNamespace(message);
        if (canHandle(operationNameAndNamespace)) {
            if (isCommitWithoutNotification(message)) {
                message = removePersisterAttributes(message);
                logger.debug("Skipping commit notification");
                // fall back to filter chain
            } else {
                Document innerResult = filterChain.execute(message, operationRouter);
                Element cfgSnapshot = getConfigSnapshot(operationRouter);
                logger.debug("Config snapshot retrieved successfully {}", cfgSnapshot);
                notificationProducer.sendCommitNotification("ok", cfgSnapshot, cap.getCapabilities());
                return innerResult;
            }
        }
        return filterChain.execute(message, operationRouter);
    }

    @Override
    public int getSortingOrder() {
        return 0;
    }

    @Override
    public int compareTo(NetconfOperationFilter o) {
        return Integer.compare(getSortingOrder(), o.getSortingOrder());
    }

    private boolean canHandle(OperationNameAndNamespace operationNameAndNamespace) {
        if (operationNameAndNamespace.getOperationName().equals("commit") == false)
            return false;
        return operationNameAndNamespace.getNamespace().equals(
                XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
    }

    private Document removePersisterAttributes(Document message) {
        final Element documentElement = message.getDocumentElement();
        documentElement.removeAttribute(NOTIFY_ATTR);
        return message;
    }

    private boolean isCommitWithoutNotification(Document message) {
        XmlElement xmlElement = XmlElement.fromDomElementWithExpected(message.getDocumentElement(),
                XmlNetconfConstants.RPC_KEY, XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        String attr = xmlElement.getAttribute(NOTIFY_ATTR);

        if (attr == null || attr.equals(""))
            return false;
        else if (attr.equals(Boolean.toString(false))) {
            logger.debug("Commit operation received with notify=false attribute {}", message);
            return true;
        } else {
            return false;
        }
    }

    private Element getConfigSnapshot(NetconfOperationRouter opRouter) throws NetconfDocumentedException {
        final Document responseDocument = opRouter.onNetconfMessage(
                getConfigMessage, null);

        XmlElement dataElement;
        try {
            XmlElement xmlElement = XmlElement.fromDomElementWithExpected(responseDocument.getDocumentElement(),
                    XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);
            dataElement = xmlElement.getOnlyChildElement(XmlNetconfConstants.DATA_KEY);
        } catch (IllegalArgumentException e) {
            final String msg = "Unexpected response from get-config operation";
            logger.warn(msg, e);
            Map<String, String> info = Maps.newHashMap();
            info.put(NetconfDocumentedException.ErrorTag.operation_failed.toString(), e.getMessage());
            throw new NetconfDocumentedException(msg, e, NetconfDocumentedException.ErrorType.application,
                    NetconfDocumentedException.ErrorTag.operation_failed,
                    NetconfDocumentedException.ErrorSeverity.error, info);
        }

        return dataElement.getDomElement();
    }

    @Override
    public String toString() {
        return "DefaultCommit{" + netconfSessionIdForReporting + '}';
    }
}
