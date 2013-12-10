/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.channel.EventLoopGroup;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Immutable
public class ConfigPusher {
    private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterNotificationHandler.class);
    private static final int NETCONF_SEND_ATTEMPT_MS_DELAY = 1000;
    private static final int NETCONF_SEND_ATTEMPTS = 20;

    private final InetSocketAddress address;
    private final EventLoopGroup nettyThreadgroup;


    public static final long DEFAULT_TIMEOUT = 120000L;// 120 seconds until netconf must be stable
    private final long timeout;

    public ConfigPusher(InetSocketAddress address, EventLoopGroup nettyThreadgroup) {
        this(address, DEFAULT_TIMEOUT, nettyThreadgroup);

    }

    public ConfigPusher(InetSocketAddress address, long timeout, EventLoopGroup nettyThreadgroup) {
        this.address = address;
        this.timeout = timeout;

        this.nettyThreadgroup = nettyThreadgroup;
    }

    public synchronized NetconfClient init(List<ConfigSnapshotHolder> configs) throws InterruptedException {
        logger.debug("Last config snapshots to be pushed to netconf: {}", configs);
        return pushAllConfigs(configs);
    }

    private synchronized NetconfClient pushAllConfigs(List<ConfigSnapshotHolder> configs) throws InterruptedException {
        NetconfClient netconfClient = makeNetconfConnection(Collections.<String>emptySet(), Optional.<NetconfClient>absent());
        for (ConfigSnapshotHolder configSnapshotHolder: configs){
            netconfClient = pushSnapshotWithRetries(configSnapshotHolder, Optional.of(netconfClient));
        }
        return netconfClient;
    }

    private synchronized NetconfClient pushSnapshotWithRetries(ConfigSnapshotHolder configSnapshotHolder,
                                                               Optional<NetconfClient> oldClientForPossibleReuse)
            throws InterruptedException {

        ConflictingVersionException lastException = null;
        int maxAttempts = 30;
        for(int i = 0 ; i < maxAttempts; i++) {
            NetconfClient netconfClient = makeNetconfConnection(configSnapshotHolder.getCapabilities(), oldClientForPossibleReuse);
            final String configSnapshot = configSnapshotHolder.getConfigSnapshot();
            logger.trace("Pushing following xml to netconf {}", configSnapshot);
            try {
                pushLastConfig(configSnapshotHolder, netconfClient);
                return netconfClient;
            } catch(ConflictingVersionException e) {
                Util.closeClientAndDispatcher(netconfClient);
                lastException = e;
                Thread.sleep(1000);
            } catch (SAXException | IOException e) {
                throw new IllegalStateException("Unable to load last config", e);
            }
        }
        throw new IllegalStateException("Failed to push configuration, maximum attempt count has been reached: "
                + maxAttempts, lastException);
    }

    /**
     * @param expectedCaps capabilities that server hello must contain. Will retry until all are found or throws RuntimeException.
     *                     If empty set is provided, will only make sure netconf client successfuly connected to the server.
     * @param oldClientForPossibleReuse if present, try to get expected capabilities from it before closing it and retrying with
     *                                  new client connection.
     * @return NetconfClient that has all required capabilities from server.
     */
    private synchronized NetconfClient makeNetconfConnection(Set<String> expectedCaps,
                                                             Optional<NetconfClient> oldClientForPossibleReuse)
            throws InterruptedException {

        if (oldClientForPossibleReuse.isPresent()) {
            NetconfClient oldClient = oldClientForPossibleReuse.get();
            if (Util.isSubset(oldClient, expectedCaps)) {
                return oldClient;
            } else {
                Util.closeClientAndDispatcher(oldClient);
            }
        }

        // TODO think about moving capability subset check to netconf client
        // could be utilized by integration tests

        long pollingStart = System.currentTimeMillis();
        int delay = 5000;

        int attempt = 0;

        long deadline = pollingStart + timeout;

        Set<String> latestCapabilities = new HashSet<>();
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            NetconfClientDispatcher netconfClientDispatcher = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup);
            NetconfClient netconfClient;
            try {
                netconfClient = new NetconfClient(this.toString(), address, delay, netconfClientDispatcher);
            } catch (IllegalStateException e) {
                logger.debug("Netconf {} was not initialized or is not stable, attempt {}", address, attempt, e);
                netconfClientDispatcher.close();
                Thread.sleep(delay);
                continue;
            }
            latestCapabilities = netconfClient.getCapabilities();
            if (Util.isSubset(netconfClient, expectedCaps)) {
                logger.debug("Hello from netconf stable with {} capabilities", latestCapabilities);
                logger.info("Session id received from netconf server: {}", netconfClient.getClientSession());
                return netconfClient;
            }
            logger.debug("Polling hello from netconf, attempt {}, capabilities {}", attempt, latestCapabilities);
            Util.closeClientAndDispatcher(netconfClient);
            Thread.sleep(delay);
        }
        Set<String> allNotFound = new HashSet<>(expectedCaps);
        allNotFound.removeAll(latestCapabilities);
        logger.error("Netconf server did not provide required capabilities. Expected but not found: {}, all expected {}, current {}",
                allNotFound, expectedCaps, latestCapabilities);
        throw new RuntimeException("Netconf server did not provide required capabilities. Expected but not found:" + allNotFound);
    }


    private synchronized void pushLastConfig(ConfigSnapshotHolder configSnapshotHolder, NetconfClient netconfClient)
            throws ConflictingVersionException, IOException, SAXException {

        Element xmlToBePersisted = XmlUtil.readXmlToElement(configSnapshotHolder.getConfigSnapshot());
        logger.info("Pushing last configuration to netconf: {}", configSnapshotHolder);
        StringBuilder response = new StringBuilder("editConfig response = {");


        NetconfMessage message = createEditConfigMessage(xmlToBePersisted, "/netconfOp/editConfig.xml");

        // sending message to netconf
        NetconfMessage responseMessage = getResponse(message, netconfClient);

        XmlElement element = XmlElement.fromDomDocument(responseMessage.getDocument());
        Preconditions.checkState(element.getName().equals(XmlNetconfConstants.RPC_REPLY_KEY));
        element = element.getOnlyChildElement();

        Util.checkIsOk(element, responseMessage);
        response.append(XmlUtil.toString(responseMessage.getDocument()));
        response.append("}");
        responseMessage = getResponse(getNetconfMessageFromResource("/netconfOp/commit.xml"), netconfClient);

        element = XmlElement.fromDomDocument(responseMessage.getDocument());
        Preconditions.checkState(element.getName().equals(XmlNetconfConstants.RPC_REPLY_KEY));
        element = element.getOnlyChildElement();

        Util.checkIsOk(element, responseMessage);
        response.append("commit response = {");
        response.append(XmlUtil.toString(responseMessage.getDocument()));
        response.append("}");
        logger.info("Last configuration loaded successfully");
        logger.trace("Detailed message {}", response);
    }

    private static NetconfMessage getResponse(NetconfMessage request, NetconfClient netconfClient) {
        try {
            return netconfClient.sendMessage(request, NETCONF_SEND_ATTEMPTS, NETCONF_SEND_ATTEMPT_MS_DELAY);
        } catch(RuntimeException e) {
            logger.error("Error while sending message {} to {}", request, netconfClient);
            throw e;
        }
    }

    private static NetconfMessage createEditConfigMessage(Element dataElement, String editConfigResourcename) {
        try (InputStream stream = ConfigPersisterNotificationHandler.class.getResourceAsStream(editConfigResourcename)) {
            Preconditions.checkNotNull(stream, "Unable to load resource " + editConfigResourcename);

            Document doc = XmlUtil.readXmlToDocument(stream);

            doc.getDocumentElement();
            XmlElement editConfigElement = XmlElement.fromDomDocument(doc).getOnlyChildElement();
            XmlElement configWrapper = editConfigElement.getOnlyChildElement(XmlNetconfConstants.CONFIG_KEY);
            editConfigElement.getDomElement().removeChild(configWrapper.getDomElement());
            for (XmlElement el : XmlElement.fromDomElement(dataElement).getChildElements()) {
                configWrapper.appendChild((Element) doc.importNode(el.getDomElement(), true));
            }
            editConfigElement.appendChild(configWrapper.getDomElement());
            return new NetconfMessage(doc);
        } catch (IOException | SAXException e) {
            throw new RuntimeException("Unable to parse message from resources " + editConfigResourcename, e);
        }
    }

    private static NetconfMessage getNetconfMessageFromResource(String resource) {
        try (InputStream stream = ConfigPusher.class.getResourceAsStream(resource)) {
            Preconditions.checkNotNull(stream, "Unable to load resource " + resource);
            return new NetconfMessage(XmlUtil.readXmlToDocument(stream));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to parse message from resources " + resource, e);
        }
    }
}
