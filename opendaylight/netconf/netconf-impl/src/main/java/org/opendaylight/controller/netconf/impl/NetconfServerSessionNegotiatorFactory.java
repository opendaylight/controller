/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.xml.XMLNetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import java.io.InputStream;

import static org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider.NetconfOperationProviderUtil.getNetconfSessionIdForReporting;

public class NetconfServerSessionNegotiatorFactory implements SessionNegotiatorFactory<NetconfHelloMessage, NetconfServerSession, NetconfServerSessionListener> {

    public static final String SERVER_HELLO_XML_LOCATION = "/server_hello.xml";

    private final Timer timer;

    private static final Document helloMessageTemplate = loadHelloMessageTemplate();
    private final SessionIdProvider idProvider;
    private final NetconfOperationProvider netconfOperationProvider;
    private final long connectionTimeoutMillis;
    private final DefaultCommitNotificationProducer commitNotificationProducer;
    private final SessionMonitoringService monitoringService;

    public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationProvider netconfOperationProvider,
                                                 SessionIdProvider idProvider, long connectionTimeoutMillis,
                                                 DefaultCommitNotificationProducer commitNot, SessionMonitoringService monitoringService) {
        this.timer = timer;
        this.netconfOperationProvider = netconfOperationProvider;
        this.idProvider = idProvider;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.commitNotificationProducer = commitNot;
        this.monitoringService = monitoringService;
    }

    private static Document loadHelloMessageTemplate() {
        InputStream resourceAsStream = NetconfServerSessionNegotiatorFactory.class
                .getResourceAsStream(SERVER_HELLO_XML_LOCATION);
        Preconditions.checkNotNull(resourceAsStream, "Unable to load server hello message blueprint from %s",
                SERVER_HELLO_XML_LOCATION);
        return NetconfUtil.createMessage(resourceAsStream).getDocument();
    }

    /**
     *
     * @param defunctSessionListenerFactory will not be taken into account as session listener factory can
     *                                      only be created after snapshot is opened, thus this method constructs
     *                                      proper session listener factory.
     * @param channel Underlying channel
     * @param promise Promise to be notified
     * @return session negotiator
     */
    @Override
    public SessionNegotiator<NetconfServerSession> getSessionNegotiator(SessionListenerFactory<NetconfServerSessionListener> defunctSessionListenerFactory,
                                                                        Channel channel, Promise<NetconfServerSession> promise) {
        long sessionId = idProvider.getNextSessionId();
        NetconfOperationServiceSnapshot netconfOperationServiceSnapshot = netconfOperationProvider.openSnapshot(
                getNetconfSessionIdForReporting(sessionId));
        CapabilityProvider capabilityProvider = new CapabilityProviderImpl(netconfOperationServiceSnapshot);

        NetconfServerSessionPreferences proposal = new NetconfServerSessionPreferences(
                createHelloMessage(sessionId, capabilityProvider), sessionId);

        NetconfServerSessionListenerFactory sessionListenerFactory = new NetconfServerSessionListenerFactory(
                commitNotificationProducer, monitoringService,
                netconfOperationServiceSnapshot, capabilityProvider);

        return new NetconfServerSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(), connectionTimeoutMillis);
    }

    private static final XPathExpression sessionIdXPath = XMLNetconfUtil
            .compileXPath("/netconf:hello/netconf:session-id");
    private static final XPathExpression capabilitiesXPath = XMLNetconfUtil
            .compileXPath("/netconf:hello/netconf:capabilities");

    private NetconfHelloMessage createHelloMessage(long sessionId, CapabilityProvider capabilityProvider) {
        Document helloMessageTemplate = getHelloTemplateClone();

        // change session ID
        final Node sessionIdNode = (Node) XmlUtil.evaluateXPath(sessionIdXPath, helloMessageTemplate,
                XPathConstants.NODE);
        sessionIdNode.setTextContent(String.valueOf(sessionId));

        // add capabilities from yang store
        final Element capabilitiesElement = (Element) XmlUtil.evaluateXPath(capabilitiesXPath, helloMessageTemplate,
                XPathConstants.NODE);

        for (String capability : capabilityProvider.getCapabilities()) {
            final Element capabilityElement = helloMessageTemplate.createElement(XmlNetconfConstants.CAPABILITY);
            capabilityElement.setTextContent(capability);
            capabilitiesElement.appendChild(capabilityElement);
        }
        return new NetconfHelloMessage(helloMessageTemplate);
    }

    private synchronized Document getHelloTemplateClone() {
        return (Document) helloMessageTemplate.cloneNode(true);
    }
}
