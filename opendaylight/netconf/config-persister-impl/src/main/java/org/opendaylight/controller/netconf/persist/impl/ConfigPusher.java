/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.persist.impl.client.ConfigPusherNetconfClient;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

@Immutable
public class ConfigPusher implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ConfigPusher.class);

    private final ConfigPusherConfiguration configuration;
    private final NetconfClientDispatcher netconfClientDispatcher;

    public ConfigPusher(ConfigPusherConfiguration configuration) {
        this.configuration = configuration;
        netconfClientDispatcher = new NetconfClientDispatcher(configuration.eventLoopGroup,
                configuration.eventLoopGroup, configuration.getAdditionalHeader(),
                configuration.connectionAttemptTimeoutMs);
    }

    public synchronized LinkedHashMap<ConfigSnapshotHolder, EditAndCommitResponseWithRetries> pushConfigs(
            List<ConfigSnapshotHolder> configs) throws InterruptedException {
        logger.debug("Last config snapshots to be pushed to netconf: {}", configs);

        // first just make sure we can connect to netconf, even if nothing is being pushed
        try(ConfigPusherNetconfClient netconfClient = makeNetconfConnection(Collections.<String>emptySet()))
        {}
        catch (ConfigPusherNetconfClient.MissingCapabilitiesException e) {
            // Cannot happen
            throw new RuntimeException(e);
        } catch (ConfigPusherNetconfClient.ConnectFailedException e) {
            throw new RuntimeException("Could not connect to netconf server on " + configuration.netconfAddress, e);
        }

        LinkedHashMap<ConfigSnapshotHolder, EditAndCommitResponseWithRetries> result = new LinkedHashMap<>();
        // start pushing snapshots:
        for (ConfigSnapshotHolder configSnapshotHolder : configs) {
            EditAndCommitResponseWithRetries editAndCommitResponseWithRetries = pushSnapshotWithRetries(configSnapshotHolder);
            logger.debug("Config snapshot pushed successfully: {}, result: {}", configSnapshotHolder, result);
            result.put(configSnapshotHolder, editAndCommitResponseWithRetries);
        }
        logger.debug("All configuration snapshots have been pushed successfully.");
        return result;
    }

    /**
     * Checks for ConflictingVersionException and retries until optimistic lock succeeds or maximal
     * number of attempts is reached.
     */
    private synchronized EditAndCommitResponseWithRetries pushSnapshotWithRetries(
            ConfigSnapshotHolder configSnapshotHolder) throws InterruptedException {

        Exception lastException = null;
        int maxAttempts = configuration.netconfPushConfigAttempts;

        for (int retryAttempt = 1; retryAttempt <= maxAttempts; retryAttempt++) {
            try (ConfigPusherNetconfClient netconfClient = makeNetconfConnection(configSnapshotHolder.getCapabilities())) {
                logger.trace("Pushing following xml to netconf {}", configSnapshotHolder);
                EditAndCommitResponse editAndCommitResponse = pushLastConfig(configSnapshotHolder, netconfClient);
                return new EditAndCommitResponseWithRetries(editAndCommitResponse, retryAttempt);
            } catch (ConflictingVersionException e) {
                logger.debug("Conflicting version detected, will retry after timeout");
                lastException = e;
                Thread.sleep(configuration.netconfPushConfigDelayMs);
            } catch (ConfigPusherNetconfClient.MissingCapabilitiesException e) {
                throw new IllegalStateException("Netconf server did not provide required capabilities on "
                        + configuration.netconfAddress, e);
            } catch (ConfigPusherNetconfClient.ConnectFailedException e) {
                throw new RuntimeException("Could not connect to netconf server on " + configuration.netconfAddress, e);
            } catch (ConfigPusherNetconfClient.SendMessageException e) {
                throw new IllegalStateException("Unable to load " + configSnapshotHolder, e);
            }
        }
        throw new IllegalStateException("Maximum attempt count has been reached for pushing " + configSnapshotHolder,
                lastException);
    }

    /**
     * @param expectedCaps capabilities that server hello must contain. Will retry until all are found or throws RuntimeException.
     *                     If empty set is provided, will only make sure netconf client successfuly connected to the server.
     * @return ConfigPusherNetconfClient that has all required capabilities from server.
     */
    private synchronized ConfigPusherNetconfClient makeNetconfConnection(Set<String> expectedCaps) throws InterruptedException, ConfigPusherNetconfClient.MissingCapabilitiesException, ConfigPusherNetconfClient.ConnectFailedException {
        final long deadlineNanos = configuration.getDeadlineForConnection();

        ConfigPusherNetconfClient netconfClient = new ConfigPusherNetconfClient(this.toString(), expectedCaps);

        int attempt = 0;
        ConfigPusherNetconfClient.MissingCapabilitiesException lastMissingCapsEx = null;

        while (System.nanoTime() < deadlineNanos) {
            try {
                Set<String> allCaps = netconfClient.connect(configuration.netconfAddress, netconfClientDispatcher,
                        configuration.getReconnectStrategy(deadlineNanos));
                logger.debug("Hello from netconf stable with {} capabilities", allCaps);
                logger.trace("Session id received from netconf server: {}", netconfClient.getClientSessionId());
                return netconfClient;
            } catch (ConfigPusherNetconfClient.ConnectFailedException e) {
                logger.error("Could not connect to the server in {} ms", configuration.netconfCapabilitiesWaitTimeoutMs);
                throw e;
            } catch (ConfigPusherNetconfClient.MissingCapabilitiesException e) {
                logger.debug("Netconf server did not provide required capabilities. Attempt {}. "
                        + "Expected but not found: {}, all expected {}, current {}", ++attempt, e.getAllNotFound(),
                        e.getExpectedCapabilities(), e.getServerCapabilities());
                lastMissingCapsEx = e;
                Thread.sleep(configuration.connectionAttemptDelayMs);
                continue;
            }
        }

        logger.error(
                "Netconf server did not provide required capabilities. Expected but not found: {}, all expected {}, current {}",
                lastMissingCapsEx.getAllNotFound(), lastMissingCapsEx.getExpectedCapabilities(),
                lastMissingCapsEx.getServerCapabilities());
        throw lastMissingCapsEx;
    }

    /**
     * Sends two RPCs to the netconf server: edit-config and commit.
     *
     * @param configSnapshotHolder
     * @param netconfClient
     * @throws ConflictingVersionException if commit fails on optimistic lock failure inside of config-manager
     * @throws java.lang.RuntimeException  if edit-config or commit fails otherwise
     */
    private synchronized EditAndCommitResponse pushLastConfig(ConfigSnapshotHolder configSnapshotHolder, ConfigPusherNetconfClient netconfClient)
            throws ConflictingVersionException, ConfigPusherNetconfClient.SendMessageException {

        Element xmlToBePersisted;
        try {
            xmlToBePersisted = XmlUtil.readXmlToElement(configSnapshotHolder.getConfigSnapshot());
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Cannot parse " + configSnapshotHolder);
        }
        logger.trace("Pushing last configuration to netconf: {}", configSnapshotHolder);

        NetconfMessage editConfigMessage = createEditConfigMessage(xmlToBePersisted);

        // sending message to netconf
        NetconfMessage editResponseMessage;
        try {
            editResponseMessage = sendRequestGetResponseCheckIsOK(editConfigMessage, netconfClient);
        } catch (ConfigPusherNetconfClient.SendMessageException e) {
            logger.warn("Edit-config failed on {}", configSnapshotHolder, e);
            throw e;
        }

        // commit
        NetconfMessage commitResponseMessage;
        try {
            commitResponseMessage = sendRequestGetResponseCheckIsOK(getCommitMessage(), netconfClient);
        } catch (ConfigPusherNetconfClient.SendMessageException e) {
            logger.warn("Edit commit succeeded, but commit failed on  {}", configSnapshotHolder, e);
            throw e;
        }

        if (logger.isTraceEnabled()) {
            StringBuilder response = new StringBuilder("editConfig response = {");
            response.append(XmlUtil.toString(editResponseMessage.getDocument()));
            response.append("}");
            response.append("commit response = {");
            response.append(XmlUtil.toString(commitResponseMessage.getDocument()));
            response.append("}");
            logger.trace("Last configuration loaded successfully");
            logger.trace("Detailed message {}", response);
        }
        return new EditAndCommitResponse(editResponseMessage, commitResponseMessage);
    }


    private NetconfMessage sendRequestGetResponseCheckIsOK(NetconfMessage request, ConfigPusherNetconfClient netconfClient)
            throws ConflictingVersionException, ConfigPusherNetconfClient.SendMessageException {
        try {
            NetconfMessage netconfMessage = netconfClient.sendMessage(request,
                    configuration.netconfSendMessageMaxAttempts * configuration.netconfSendMessageDelayMs);
            NetconfUtil.checkIsMessageOk(netconfMessage);
            return netconfMessage;
        } catch (ConflictingVersionException e) {
            logger.trace("Conflicting version detected: {}", e.toString());
            throw e;
        } catch (ConfigPusherNetconfClient.SendMessageException e) {
            logger.debug("Error while executing netconf transaction {} to {}", request, netconfClient, e);
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // load editConfig.xml template, populate /rpc/edit-config/config with parameter
    private static NetconfMessage createEditConfigMessage(Element dataElement) {
        String editConfigResourcePath = "/netconfOp/editConfig.xml";
        try (InputStream stream = ConfigPersisterNotificationHandler.class.getResourceAsStream(editConfigResourcePath)) {
            Preconditions.checkNotNull(stream, "Unable to load resource " + editConfigResourcePath);

            Document doc = XmlUtil.readXmlToDocument(stream);

            XmlElement editConfigElement = XmlElement.fromDomDocument(doc).getOnlyChildElement();
            XmlElement configWrapper = editConfigElement.getOnlyChildElement(XmlNetconfConstants.CONFIG_KEY);
            editConfigElement.getDomElement().removeChild(configWrapper.getDomElement());
            for (XmlElement el : XmlElement.fromDomElement(dataElement).getChildElements()) {
                boolean deep = true;
                configWrapper.appendChild((Element) doc.importNode(el.getDomElement(), deep));
            }
            editConfigElement.appendChild(configWrapper.getDomElement());
            return new NetconfMessage(doc);
        } catch (IOException | SAXException e) {
            // error reading the xml file bundled into the jar
            throw new RuntimeException("Error while opening local resource " + editConfigResourcePath, e);
        }
    }

    private static NetconfMessage getCommitMessage() {
        String resource = "/netconfOp/commit.xml";
        try (InputStream stream = ConfigPusher.class.getResourceAsStream(resource)) {
            Preconditions.checkNotNull(stream, "Unable to load resource " + resource);
            return new NetconfMessage(XmlUtil.readXmlToDocument(stream));
        } catch (SAXException | IOException e) {
            // error reading the xml file bundled into the jar
            throw new RuntimeException("Error while opening local resource " + resource, e);
        }
    }

    @Override
    public void close() {
        try {
            netconfClientDispatcher.close();
        } catch (Exception e) {
            logger.warn("Ignoring exception while closing {}", netconfClientDispatcher, e);
        }
    }

    static class EditAndCommitResponse {
        private final NetconfMessage editResponse, commitResponse;

        EditAndCommitResponse(NetconfMessage editResponse, NetconfMessage commitResponse) {
            this.editResponse = editResponse;
            this.commitResponse = commitResponse;
        }

        public NetconfMessage getEditResponse() {
            return editResponse;
        }

        public NetconfMessage getCommitResponse() {
            return commitResponse;
        }

        @Override
        public String toString() {
            return "EditAndCommitResponse{" +
                    "editResponse=" + editResponse +
                    ", commitResponse=" + commitResponse +
                    '}';
        }
    }


    static class EditAndCommitResponseWithRetries {
        private final EditAndCommitResponse editAndCommitResponse;
        private final int retries;

        EditAndCommitResponseWithRetries(EditAndCommitResponse editAndCommitResponse, int retries) {
            this.editAndCommitResponse = editAndCommitResponse;
            this.retries = retries;
        }

        public int getRetries() {
            return retries;
        }

        public EditAndCommitResponse getEditAndCommitResponse() {
            return editAndCommitResponse;
        }

        @Override
        public String toString() {
            return "EditAndCommitResponseWithRetries{" +
                    "editAndCommitResponse=" + editAndCommitResponse +
                    ", retries=" + retries +
                    '}';
        }
    }

}
