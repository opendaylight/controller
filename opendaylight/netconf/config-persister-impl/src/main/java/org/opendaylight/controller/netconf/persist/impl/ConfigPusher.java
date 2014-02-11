/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Immutable
public class ConfigPusher {
    private static final Logger logger = LoggerFactory.getLogger(ConfigPusher.class);

    private final ConfigPusherConfiguration configuration;

    public ConfigPusher(ConfigPusherConfiguration configuration) {
        this.configuration = configuration;
    }

    public synchronized LinkedHashMap<ConfigSnapshotHolder, EditAndCommitResponseWithRetries> pushConfigs(
            List<ConfigSnapshotHolder> configs) throws InterruptedException {
        logger.debug("Last config snapshots to be pushed to netconf: {}", configs);

        // first just make sure we can connect to netconf, even if nothing is being pushed
        {
            NetconfClient netconfClient = makeNetconfConnection(Collections.<String>emptySet());
            Util.closeClientAndDispatcher(netconfClient);
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
    private synchronized EditAndCommitResponseWithRetries pushSnapshotWithRetries(ConfigSnapshotHolder configSnapshotHolder)
            throws InterruptedException {

        ConflictingVersionException lastException = null;
        int maxAttempts = configuration.netconfPushConfigAttempts;

        for (int retryAttempt = 1; retryAttempt <= maxAttempts; retryAttempt++) {
            NetconfClient netconfClient = makeNetconfConnection(configSnapshotHolder.getCapabilities());
            logger.trace("Pushing following xml to netconf {}", configSnapshotHolder);
            try {
                EditAndCommitResponse editAndCommitResponse = pushLastConfig(configSnapshotHolder, netconfClient);
                return new EditAndCommitResponseWithRetries(editAndCommitResponse, retryAttempt);
            } catch (ConflictingVersionException e) {
                logger.debug("Conflicting version detected, will retry after timeout");
                lastException = e;
                Thread.sleep(configuration.netconfPushConfigDelayMs);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Unable to load " + configSnapshotHolder, e);
            } finally {
                Util.closeClientAndDispatcher(netconfClient);
            }
        }
        throw new IllegalStateException("Maximum attempt count has been reached for pushing " + configSnapshotHolder,
                lastException);
    }

    /**
     * @param expectedCaps capabilities that server hello must contain. Will retry until all are found or throws RuntimeException.
     *                     If empty set is provided, will only make sure netconf client successfuly connected to the server.
     * @return NetconfClient that has all required capabilities from server.
     */
    private synchronized NetconfClient makeNetconfConnection(Set<String> expectedCaps) throws InterruptedException {

        // TODO think about moving capability subset check to netconf client
        // could be utilized by integration tests

        final long pollingStartNanos = System.nanoTime();
        final long deadlineNanos = pollingStartNanos + TimeUnit.MILLISECONDS.toNanos(configuration.netconfCapabilitiesWaitTimeoutMs);
        int attempt = 0;

        NetconfHelloMessageAdditionalHeader additionalHeader = new NetconfHelloMessageAdditionalHeader("unknown",
                configuration.netconfAddress.getAddress().getHostAddress(),
                Integer.toString(configuration.netconfAddress.getPort()), "tcp", "persister");

        Set<String> latestCapabilities = null;
        while (System.nanoTime() < deadlineNanos) {
            attempt++;
            NetconfClientDispatcher netconfClientDispatcher = new NetconfClientDispatcher(configuration.eventLoopGroup,
                    configuration.eventLoopGroup, additionalHeader, configuration.connectionAttemptTimeoutMs);
            NetconfClient netconfClient;
            try {
                netconfClient = new NetconfClient(this.toString(), configuration.netconfAddress, configuration.connectionAttemptDelayMs, netconfClientDispatcher);
            } catch (IllegalStateException e) {
                logger.debug("Netconf {} was not initialized or is not stable, attempt {}", configuration.netconfAddress, attempt, e);
                netconfClientDispatcher.close();
                Thread.sleep(configuration.connectionAttemptDelayMs);
                continue;
            }
            latestCapabilities = netconfClient.getCapabilities();
            if (Util.isSubset(netconfClient, expectedCaps)) {
                logger.debug("Hello from netconf stable with {} capabilities", latestCapabilities);
                logger.trace("Session id received from netconf server: {}", netconfClient.getClientSession());
                return netconfClient;
            }
            Set<String> allNotFound = computeNotFoundCapabilities(expectedCaps, latestCapabilities);
            logger.debug("Netconf server did not provide required capabilities. Attempt {}. " +
                    "Expected but not found: {}, all expected {}, current {}",
                    attempt, allNotFound, expectedCaps, latestCapabilities);
            Util.closeClientAndDispatcher(netconfClient);
            Thread.sleep(configuration.connectionAttemptDelayMs);
        }
        if (latestCapabilities == null) {
            logger.error("Could not connect to the server in {} ms", configuration.netconfCapabilitiesWaitTimeoutMs);
            throw new RuntimeException("Could not connect to netconf server");
        }
        Set<String> allNotFound = computeNotFoundCapabilities(expectedCaps, latestCapabilities);
        logger.error("Netconf server did not provide required capabilities. Expected but not found: {}, all expected {}, current {}",
                allNotFound, expectedCaps, latestCapabilities);
        throw new RuntimeException("Netconf server did not provide required capabilities. Expected but not found:" + allNotFound);
    }

    private static Set<String> computeNotFoundCapabilities(Set<String> expectedCaps, Set<String> latestCapabilities) {
        Set<String> allNotFound = new HashSet<>(expectedCaps);
        allNotFound.removeAll(latestCapabilities);
        return allNotFound;
    }


    /**
     * Sends two RPCs to the netconf server: edit-config and commit.
     *
     * @param configSnapshotHolder
     * @param netconfClient
     * @throws ConflictingVersionException if commit fails on optimistic lock failure inside of config-manager
     * @throws java.lang.RuntimeException  if edit-config or commit fails otherwise
     */
    private synchronized EditAndCommitResponse pushLastConfig(ConfigSnapshotHolder configSnapshotHolder, NetconfClient netconfClient)
            throws ConflictingVersionException {

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
        } catch (IOException e) {
            throw new IllegalStateException("Edit-config failed on " + configSnapshotHolder, e);
        }

        // commit
        NetconfMessage commitResponseMessage;
        try {
            commitResponseMessage = sendRequestGetResponseCheckIsOK(getCommitMessage(), netconfClient);
        } catch (IOException e) {
            throw new IllegalStateException("Edit commit succeeded, but commit failed on " + configSnapshotHolder, e);
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


    private NetconfMessage sendRequestGetResponseCheckIsOK(NetconfMessage request, NetconfClient netconfClient)
            throws ConflictingVersionException, IOException {
        try {
            NetconfMessage netconfMessage = netconfClient.sendMessage(request,
                    configuration.netconfSendMessageMaxAttempts, configuration.netconfSendMessageDelayMs);
            NetconfUtil.checkIsMessageOk(netconfMessage);
            return netconfMessage;
        } catch(ConflictingVersionException e) {
            logger.trace("conflicting version detected: {}", e.toString());
            throw e;
        } catch (RuntimeException | ExecutionException | InterruptedException | TimeoutException e) { // TODO: change NetconfClient#sendMessage to throw checked exceptions
            logger.debug("Error while executing netconf transaction {} to {}", request, netconfClient, e);
            throw new IOException("Failed to execute netconf transaction", e);
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
