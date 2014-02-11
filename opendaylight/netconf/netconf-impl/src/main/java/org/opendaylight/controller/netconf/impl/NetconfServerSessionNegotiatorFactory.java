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
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
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

public class NetconfServerSessionNegotiatorFactory implements SessionNegotiatorFactory<NetconfHelloMessage, NetconfServerSession, NetconfServerSessionListener> {

    public static final String SERVER_HELLO_XML_LOCATION = "/server_hello.xml";

    private final Timer timer;

    private static final Document helloMessageTemplate = loadHelloMessageTemplate();
    private final SessionIdProvider idProvider;
    private final NetconfOperationServiceFactoryListener factoriesListener;
    private final long connectionTimeoutMillis;

    public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationServiceFactoryListener factoriesListener,
            SessionIdProvider idProvider, long connectionTimeoutMillis) {
        this.timer = timer;
        this.factoriesListener = factoriesListener;
        this.idProvider = idProvider;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    private static Document loadHelloMessageTemplate() {
        InputStream resourceAsStream = NetconfServerSessionNegotiatorFactory.class
                .getResourceAsStream(SERVER_HELLO_XML_LOCATION);
        Preconditions.checkNotNull(resourceAsStream, "Unable to load server hello message blueprint from %s",
                SERVER_HELLO_XML_LOCATION);
        return NetconfUtil.createMessage(resourceAsStream).getDocument();
    }

    @Override
    public SessionNegotiator<NetconfServerSession> getSessionNegotiator(SessionListenerFactory<NetconfServerSessionListener> sessionListenerFactory, Channel channel,
            Promise<NetconfServerSession> promise) {
        long sessionId = idProvider.getNextSessionId();

        NetconfServerSessionPreferences proposal = new NetconfServerSessionPreferences(createHelloMessage(sessionId),
                sessionId);
        return new NetconfServerSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(), connectionTimeoutMillis);
    }

    private static final XPathExpression sessionIdXPath = XMLNetconfUtil
            .compileXPath("/netconf:hello/netconf:session-id");
    private static final XPathExpression capabilitiesXPath = XMLNetconfUtil
            .compileXPath("/netconf:hello/netconf:capabilities");

    private NetconfHelloMessage createHelloMessage(long sessionId) {
        Document helloMessageTemplate = getHelloTemplateClone();

        // change session ID
        final Node sessionIdNode = (Node) XmlUtil.evaluateXPath(sessionIdXPath, helloMessageTemplate,
                XPathConstants.NODE);
        sessionIdNode.setTextContent(String.valueOf(sessionId));

        // add capabilities from yang store
        final Element capabilitiesElement = (Element) XmlUtil.evaluateXPath(capabilitiesXPath, helloMessageTemplate,
                XPathConstants.NODE);

        CapabilityProvider capabilityProvider = new CapabilityProviderImpl(factoriesListener.getSnapshot(sessionId));

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
