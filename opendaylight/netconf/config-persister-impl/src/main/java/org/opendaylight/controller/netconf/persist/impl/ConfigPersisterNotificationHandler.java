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
import com.google.common.collect.Sets;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.jmx.CommitJMXNotification;
import org.opendaylight.controller.netconf.api.jmx.DefaultCommitOperationMXBean;
import org.opendaylight.controller.netconf.api.jmx.NetconfJMXNotification;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.util.xml.XMLNetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Responsible for listening for notifications from netconf containing latest
 * committed configuration that should be persisted, and also for loading last
 * configuration.
 */
@ThreadSafe
public class ConfigPersisterNotificationHandler implements NotificationListener, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterNotificationHandler.class);
    private static final int NETCONF_SEND_ATTEMPT_MS_DELAY = 1000;
    private static final int NETCONF_SEND_ATTEMPTS = 20;

    private final InetSocketAddress address;
    private final EventLoopGroup nettyThreadgroup;

    private NetconfClientDispatcher netconfClientDispatcher;
    private NetconfClient netconfClient;

    private final Persister persister;
    private final MBeanServerConnection mbeanServer;


    private final ObjectName on = DefaultCommitOperationMXBean.objectName;

    public static final long DEFAULT_TIMEOUT = 120000L;// 120 seconds until netconf must be stable
    private final long timeout;
    private final Pattern ignoredMissingCapabilityRegex;

    public ConfigPersisterNotificationHandler(Persister persister, InetSocketAddress address,
            MBeanServerConnection mbeanServer, Pattern ignoredMissingCapabilityRegex) {
        this(persister, address, mbeanServer, DEFAULT_TIMEOUT, ignoredMissingCapabilityRegex);

    }

    public ConfigPersisterNotificationHandler(Persister persister, InetSocketAddress address,
            MBeanServerConnection mbeanServer, long timeout, Pattern ignoredMissingCapabilityRegex) {
        this.persister = persister;
        this.address = address;
        this.mbeanServer = mbeanServer;
        this.timeout = timeout;

        this.nettyThreadgroup = new NioEventLoopGroup();
        this.ignoredMissingCapabilityRegex = ignoredMissingCapabilityRegex;
    }

    public void init() throws InterruptedException {
        Optional<ConfigSnapshotHolder> maybeConfig = loadLastConfig();

        if (maybeConfig.isPresent()) {
            logger.debug("Last config found {}", persister);
            ConflictingVersionException lastException = null;
            pushLastConfigWithRetries(maybeConfig, lastException);

        } else {
            // this ensures that netconf is initialized, this is first
            // connection
            // this means we can register as listener for commit
            registerToNetconf(Collections.<String>emptySet());

            logger.info("No last config provided by backend storage {}", persister);
        }
        registerAsJMXListener();
    }

    private void pushLastConfigWithRetries(Optional<ConfigSnapshotHolder> maybeConfig, ConflictingVersionException lastException) throws InterruptedException {
        int maxAttempts = 30;
        for(int i = 0 ; i < maxAttempts; i++) {
            registerToNetconf(maybeConfig.get().getCapabilities());

            final String configSnapshot = maybeConfig.get().getConfigSnapshot();
            logger.trace("Pushing following xml to netconf {}", configSnapshot);
            try {
                pushLastConfig(XmlUtil.readXmlToElement(configSnapshot));
                return;
            } catch(ConflictingVersionException e) {
                closeClientAndDispatcher(netconfClient, netconfClientDispatcher);
                lastException = e;
                Thread.sleep(1000);
            } catch (SAXException | IOException e) {
                throw new IllegalStateException("Unable to load last config", e);
            }
        }
        throw new IllegalStateException("Failed to push configuration, maximum attempt count has been reached: "
                + maxAttempts, lastException);
    }

    private synchronized long registerToNetconf(Set<String> expectedCaps) throws InterruptedException {

        Set<String> currentCapabilities = Sets.newHashSet();

        // TODO think about moving capability subset check to netconf client
        // could be utilized by integration tests

        long pollingStart = System.currentTimeMillis();
        int delay = 5000;

        int attempt = 0;

        long deadline = pollingStart + timeout;
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            netconfClientDispatcher = new NetconfClientDispatcher(Optional.<SSLContext>absent(), nettyThreadgroup, nettyThreadgroup);
            try {
                netconfClient = new NetconfClient(this.toString(), address, delay, netconfClientDispatcher);
            } catch (IllegalStateException e) {
                logger.debug("Netconf {} was not initialized or is not stable, attempt {}", address, attempt, e);
                netconfClientDispatcher.close();
                Thread.sleep(delay);
                continue;
            }
            currentCapabilities = netconfClient.getCapabilities();

            if (isSubset(currentCapabilities, expectedCaps)) {
                logger.debug("Hello from netconf stable with {} capabilities", currentCapabilities);
                long currentSessionId = netconfClient.getSessionId();
                logger.info("Session id received from netconf server: {}", currentSessionId);
                return currentSessionId;
            }



            logger.debug("Polling hello from netconf, attempt {}, capabilities {}", attempt, currentCapabilities);

            closeClientAndDispatcher(netconfClient, netconfClientDispatcher);

            Thread.sleep(delay);
        }
        Set<String> allNotFound = new HashSet<>(expectedCaps);
        allNotFound.removeAll(currentCapabilities);
        logger.error("Netconf server did not provide required capabilities. Expected but not found: {}, all expected {}, current {}",
                allNotFound, expectedCaps ,currentCapabilities);
        throw new RuntimeException("Netconf server did not provide required capabilities. Expected but not found:" + allNotFound);

    }

    private static void closeClientAndDispatcher(Closeable client, Closeable dispatcher) {
        Exception fromClient = null;
        try {
            client.close();
        } catch (Exception e) {
            fromClient = e;
        } finally {
            try {
                dispatcher.close();
            } catch (Exception e) {
                if (fromClient != null) {
                    e.addSuppressed(fromClient);
                }

                throw new RuntimeException("Error closing temporary client ", e);
            }
        }
    }

    private boolean isSubset(Set<String> currentCapabilities, Set<String> expectedCaps) {
        for (String exCap : expectedCaps) {
            if (currentCapabilities.contains(exCap) == false)
                return false;
        }
        return true;
    }

    private void registerAsJMXListener() {
        logger.trace("Called registerAsJMXListener");
        try {
            mbeanServer.addNotificationListener(on, this, null, null);
        } catch (InstanceNotFoundException | IOException e) {
            throw new RuntimeException("Cannot register as JMX listener to netconf", e);
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (notification instanceof NetconfJMXNotification == false)
            return;

        // Socket should not be closed at this point
        // Activator unregisters this as JMX listener before close is called

        logger.debug("Received notification {}", notification);
        if (notification instanceof CommitJMXNotification) {
            try {
                handleAfterCommitNotification((CommitJMXNotification) notification);
            } catch (Exception e) {
                // TODO: notificationBroadcast support logs only DEBUG
                logger.warn("Exception occured during notification handling: ", e);
                throw e;
            }
        } else
            throw new IllegalStateException("Unknown config registry notification type " + notification);
    }

    private void handleAfterCommitNotification(final CommitJMXNotification notification) {
        try {
            persister.persistConfig(new CapabilityStrippingConfigSnapshotHolder(notification.getConfigSnapshot(),
                    notification.getCapabilities(), ignoredMissingCapabilityRegex));
            logger.debug("Configuration persisted successfully");
        } catch (IOException e) {
            throw new RuntimeException("Unable to persist configuration snapshot", e);
        }
    }

    private Optional<ConfigSnapshotHolder> loadLastConfig() {
        Optional<ConfigSnapshotHolder> maybeConfigElement;
        try {
            maybeConfigElement = persister.loadLastConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
        return maybeConfigElement;
    }

    private synchronized void pushLastConfig(Element xmlToBePersisted) throws ConflictingVersionException {
        logger.info("Pushing last configuration to netconf");
        StringBuilder response = new StringBuilder("editConfig response = {");


        NetconfMessage message = createEditConfigMessage(xmlToBePersisted, "/netconfOp/editConfig.xml");

        // sending message to netconf
        NetconfMessage responseMessage = netconfClient.sendMessage(message, NETCONF_SEND_ATTEMPTS, NETCONF_SEND_ATTEMPT_MS_DELAY);

        XmlElement element = XmlElement.fromDomDocument(responseMessage.getDocument());
        Preconditions.checkState(element.getName().equals(XmlNetconfConstants.RPC_REPLY_KEY));
        element = element.getOnlyChildElement();

        checkIsOk(element, responseMessage);
        response.append(XmlUtil.toString(responseMessage.getDocument()));
        response.append("}");
        responseMessage = netconfClient.sendMessage(getNetconfMessageFromResource("/netconfOp/commit.xml"), NETCONF_SEND_ATTEMPTS, NETCONF_SEND_ATTEMPT_MS_DELAY);

        element = XmlElement.fromDomDocument(responseMessage.getDocument());
        Preconditions.checkState(element.getName().equals(XmlNetconfConstants.RPC_REPLY_KEY));
        element = element.getOnlyChildElement();

        checkIsOk(element, responseMessage);
        response.append("commit response = {");
        response.append(XmlUtil.toString(responseMessage.getDocument()));
        response.append("}");
        logger.info("Last configuration loaded successfully");
        logger.trace("Detailed message {}", response);
    }

    static void checkIsOk(XmlElement element, NetconfMessage responseMessage) throws ConflictingVersionException {
        if (element.getName().equals(XmlNetconfConstants.OK)) {
            return;
        }

        if (element.getName().equals(XmlNetconfConstants.RPC_ERROR)) {
            logger.warn("Can not load last configuration, operation failed");
            // is it ConflictingVersionException ?
            XPathExpression xPathExpression = XMLNetconfUtil.compileXPath("/netconf:rpc-reply/netconf:rpc-error/netconf:error-info/netconf:error");
            String error = (String) XmlUtil.evaluateXPath(xPathExpression, element.getDomElement(), XPathConstants.STRING);
            if (error!=null && error.contains(ConflictingVersionException.class.getCanonicalName())) {
                throw new ConflictingVersionException(error);
            }
            throw new IllegalStateException("Can not load last configuration, operation failed: "
                    + XmlUtil.toString(responseMessage.getDocument()));
        }

        logger.warn("Can not load last configuration. Operation failed.");
        throw new IllegalStateException("Can not load last configuration. Operation failed: "
                + XmlUtil.toString(responseMessage.getDocument()));
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

    private NetconfMessage getNetconfMessageFromResource(String resource) {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            Preconditions.checkNotNull(stream, "Unable to load resource " + resource);
            return new NetconfMessage(XmlUtil.readXmlToDocument(stream));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to parse message from resources " + resource, e);
        }
    }

    @Override
    public synchronized void close() {
        // TODO persister is received from constructor, should not be closed
        // here
        try {
            persister.close();
        } catch (Exception e) {
            logger.warn("Unable to close config persister {}", persister, e);
        }

        if (netconfClient != null) {
            try {
                netconfClient.close();
            } catch (Exception e) {
                logger.warn("Unable to close connection to netconf {}", netconfClient, e);
            }
        }

        if (netconfClientDispatcher != null) {
            try {
                netconfClientDispatcher.close();
            } catch (Exception e) {
                logger.warn("Unable to close connection to netconf {}", netconfClientDispatcher, e);
            }
        }

        try {
            nettyThreadgroup.shutdownGracefully();
        } catch (Exception e) {
            logger.warn("Unable to close netconf client thread group {}", netconfClientDispatcher, e);
        }

        // unregister from JMX
        try {
            if (mbeanServer.isRegistered(on)) {
                mbeanServer.removeNotificationListener(on, this);
            }
        } catch (Exception e) {
            logger.warn("Unable to unregister {} as listener for {}", this, on, e);
        }
    }
}
