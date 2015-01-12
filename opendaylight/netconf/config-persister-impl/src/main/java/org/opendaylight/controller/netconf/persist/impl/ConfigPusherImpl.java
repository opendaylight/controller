/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServerConnection;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@Immutable
public class ConfigPusherImpl implements ConfigPusher {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigPusherImpl.class);

    private final long maxWaitForCapabilitiesMillis;
    private final long conflictingVersionTimeoutMillis;
    private final NetconfOperationServiceFactory configNetconfConnector;
    private static final int QUEUE_SIZE = 100;
    private BlockingQueue<List<? extends ConfigSnapshotHolder>> queue = new LinkedBlockingQueue<List<? extends ConfigSnapshotHolder>>(QUEUE_SIZE);

    public ConfigPusherImpl(NetconfOperationServiceFactory configNetconfConnector, long maxWaitForCapabilitiesMillis,
                        long conflictingVersionTimeoutMillis) {
        this.configNetconfConnector = configNetconfConnector;
        this.maxWaitForCapabilitiesMillis = maxWaitForCapabilitiesMillis;
        this.conflictingVersionTimeoutMillis = conflictingVersionTimeoutMillis;
    }

    public void process(List<AutoCloseable> autoCloseables, MBeanServerConnection platformMBeanServer, Persister persisterAggregator) throws InterruptedException {
        List<? extends ConfigSnapshotHolder> configs;
        while(true) {
            configs = queue.take();
            try {
                internalPushConfigs(configs);
                ConfigPersisterNotificationHandler jmxNotificationHandler = new ConfigPersisterNotificationHandler(platformMBeanServer, persisterAggregator);
                synchronized (autoCloseables) {
                    autoCloseables.add(jmxNotificationHandler);
                }

                LOG.debug("ConfigPusher has pushed configs {}", configs);
            } catch (NetconfDocumentedException e) {
                LOG.error("Error pushing configs {}",configs);
                throw new IllegalStateException(e);
            }
        }
    }

    public void pushConfigs(List<? extends ConfigSnapshotHolder> configs) throws InterruptedException {
        LOG.debug("Requested to push configs {}", configs);
        this.queue.put(configs);
    }

    private LinkedHashMap<? extends ConfigSnapshotHolder, EditAndCommitResponse> internalPushConfigs(List<? extends ConfigSnapshotHolder> configs) throws NetconfDocumentedException {
        LOG.debug("Last config snapshots to be pushed to netconf: {}", configs);
        LinkedHashMap<ConfigSnapshotHolder, EditAndCommitResponse> result = new LinkedHashMap<>();
        // start pushing snapshots:
        for (ConfigSnapshotHolder configSnapshotHolder : configs) {
            if(configSnapshotHolder != null) {
                EditAndCommitResponse editAndCommitResponseWithRetries = pushConfigWithConflictingVersionRetries(configSnapshotHolder);
                LOG.debug("Config snapshot pushed successfully: {}, result: {}", configSnapshotHolder, result);
                result.put(configSnapshotHolder, editAndCommitResponseWithRetries);
            }
        }
        LOG.debug("All configuration snapshots have been pushed successfully.");
        return result;
    }

    /**
     * First calls {@link #getOperationServiceWithRetries(java.util.Set, String)} in order to wait until
     * expected capabilities are present, then tries to push configuration. If {@link ConflictingVersionException}
     * is caught, whole process is retried - new service instance need to be obtained from the factory. Closes
     * {@link NetconfOperationService} after each use.
     */
    private synchronized EditAndCommitResponse pushConfigWithConflictingVersionRetries(ConfigSnapshotHolder configSnapshotHolder) throws NetconfDocumentedException {
        ConflictingVersionException lastException;
        Stopwatch stopwatch = new Stopwatch();
        do {
            String idForReporting = configSnapshotHolder.toString();
            SortedSet<String> expectedCapabilities = checkNotNull(configSnapshotHolder.getCapabilities(),
                    "Expected capabilities must not be null - %s, check %s", idForReporting,
                    configSnapshotHolder.getClass().getName());
            try (NetconfOperationService operationService = getOperationServiceWithRetries(expectedCapabilities, idForReporting)) {
                if(!stopwatch.isRunning()) {
                    stopwatch.start();
                }
                return pushConfig(configSnapshotHolder, operationService);
            } catch (ConflictingVersionException e) {
                lastException = e;
                LOG.info("Conflicting version detected, will retry after timeout");
                sleep();
            }
        } while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < conflictingVersionTimeoutMillis);
        throw new IllegalStateException("Max wait for conflicting version stabilization timeout after " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms",
                lastException);
    }

    private NetconfOperationService getOperationServiceWithRetries(Set<String> expectedCapabilities, String idForReporting) {
        Stopwatch stopwatch = new Stopwatch().start();
        NotEnoughCapabilitiesException lastException;
        do {
            try {
                return getOperationService(expectedCapabilities, idForReporting);
            } catch (NotEnoughCapabilitiesException e) {
                LOG.debug("Not enough capabilities: {}", e.toString());
                lastException = e;
                sleep();
            }
        } while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < maxWaitForCapabilitiesMillis);
        throw new IllegalStateException("Max wait for capabilities reached." + lastException.getMessage(), lastException);
    }

    private static class NotEnoughCapabilitiesException extends Exception {
        private static final long serialVersionUID = 1L;

        private NotEnoughCapabilitiesException(String message, Throwable cause) {
            super(message, cause);
        }

        private NotEnoughCapabilitiesException(String message) {
            super(message);
        }
    }

    /**
     * Get NetconfOperationService iif all required capabilities are present.
     *
     * @param expectedCapabilities that must be provided by configNetconfConnector
     * @param idForReporting
     * @return service if capabilities are present, otherwise absent value
     */
    private NetconfOperationService getOperationService(Set<String> expectedCapabilities, String idForReporting) throws NotEnoughCapabilitiesException {
        NetconfOperationService serviceCandidate;
        try {
            serviceCandidate = configNetconfConnector.createService(idForReporting);
        } catch(RuntimeException e) {
            throw new NotEnoughCapabilitiesException("Netconf service not stable for " + idForReporting, e);
        }
        Set<String> notFoundDiff = computeNotFoundCapabilities(expectedCapabilities, serviceCandidate);
        if (notFoundDiff.isEmpty()) {
            return serviceCandidate;
        } else {
            serviceCandidate.close();
            LOG.trace("Netconf server did not provide required capabilities for {} ", idForReporting,
                    "Expected but not found: {}, all expected {}, current {}",
                     notFoundDiff, expectedCapabilities, serviceCandidate.getCapabilities()
            );
            throw new NotEnoughCapabilitiesException("Not enough capabilities for " + idForReporting + ". Expected but not found: " + notFoundDiff);
        }
    }

    private static Set<String> computeNotFoundCapabilities(Set<String> expectedCapabilities, NetconfOperationService serviceCandidate) {
        Collection<String> actual = Collections2.transform(serviceCandidate.getCapabilities(), new Function<Capability, String>() {
            @Override
            public String apply(@Nonnull final Capability input) {
                return input.getCapabilityUri();
            }
        });
        Set<String> allNotFound = new HashSet<>(expectedCapabilities);
        allNotFound.removeAll(actual);
        return allNotFound;
    }



    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Sends two RPCs to the netconf server: edit-config and commit.
     *
     * @param configSnapshotHolder
     * @throws ConflictingVersionException if commit fails on optimistic lock failure inside of config-manager
     * @throws java.lang.RuntimeException  if edit-config or commit fails otherwise
     */
    private synchronized EditAndCommitResponse pushConfig(ConfigSnapshotHolder configSnapshotHolder, NetconfOperationService operationService)
            throws ConflictingVersionException, NetconfDocumentedException {

        Element xmlToBePersisted;
        try {
            xmlToBePersisted = XmlUtil.readXmlToElement(configSnapshotHolder.getConfigSnapshot());
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Cannot parse " + configSnapshotHolder);
        }
        LOG.trace("Pushing last configuration to netconf: {}", configSnapshotHolder);
        Stopwatch stopwatch = new Stopwatch().start();
        NetconfMessage editConfigMessage = createEditConfigMessage(xmlToBePersisted);

        Document editResponseMessage = sendRequestGetResponseCheckIsOK(editConfigMessage, operationService,
                "edit-config", configSnapshotHolder.toString());

        Document commitResponseMessage = sendRequestGetResponseCheckIsOK(getCommitMessage(), operationService,
                "commit", configSnapshotHolder.toString());

        if (LOG.isTraceEnabled()) {
            StringBuilder response = new StringBuilder("editConfig response = {");
            response.append(XmlUtil.toString(editResponseMessage));
            response.append("}");
            response.append("commit response = {");
            response.append(XmlUtil.toString(commitResponseMessage));
            response.append("}");
            LOG.trace("Last configuration loaded successfully");
            LOG.trace("Detailed message {}", response);
            LOG.trace("Total time spent {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
        return new EditAndCommitResponse(editResponseMessage, commitResponseMessage);
    }

    private NetconfOperation findOperation(NetconfMessage request, NetconfOperationService operationService) throws NetconfDocumentedException {
        TreeMap<HandlingPriority, NetconfOperation> allOperations = new TreeMap<>();
        Set<NetconfOperation> netconfOperations = operationService.getNetconfOperations();
        if (netconfOperations.isEmpty()) {
            throw new IllegalStateException("Possible code error: no config operations");
        }
        for (NetconfOperation netconfOperation : netconfOperations) {
            HandlingPriority handlingPriority = netconfOperation.canHandle(request.getDocument());
            allOperations.put(handlingPriority, netconfOperation);
        }
        Entry<HandlingPriority, NetconfOperation> highestEntry = allOperations.lastEntry();
        if (highestEntry.getKey().isCannotHandle()) {
            throw new IllegalStateException("Possible code error: operation with highest priority is CANNOT_HANDLE");
        }
        return highestEntry.getValue();
    }

    private Document sendRequestGetResponseCheckIsOK(NetconfMessage request, NetconfOperationService operationService,
                                                     String operationNameForReporting, String configIdForReporting)
            throws ConflictingVersionException, NetconfDocumentedException {

        NetconfOperation operation = findOperation(request, operationService);
        Document response;
        try {
            response = operation.handle(request.getDocument(), NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);
        } catch (NetconfDocumentedException | RuntimeException e) {
            if (e instanceof NetconfDocumentedException && e.getCause() instanceof ConflictingVersionException) {
                throw (ConflictingVersionException) e.getCause();
            }
            throw new IllegalStateException("Failed to send " + operationNameForReporting +
                    " for configuration " + configIdForReporting, e);
        }
        return NetconfUtil.checkIsMessageOk(response);
    }

    // load editConfig.xml template, populate /rpc/edit-config/config with parameter
    private static NetconfMessage createEditConfigMessage(Element dataElement) throws NetconfDocumentedException {
        String editConfigResourcePath = "/netconfOp/editConfig.xml";
        try (InputStream stream = ConfigPersisterNotificationHandler.class.getResourceAsStream(editConfigResourcePath)) {
            checkNotNull(stream, "Unable to load resource " + editConfigResourcePath);

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
            throw new IllegalStateException("Error while opening local resource " + editConfigResourcePath, e);
        }
    }

    private static NetconfMessage getCommitMessage() {
        String resource = "/netconfOp/commit.xml";
        try (InputStream stream = ConfigPusherImpl.class.getResourceAsStream(resource)) {
            checkNotNull(stream, "Unable to load resource " + resource);
            return new NetconfMessage(XmlUtil.readXmlToDocument(stream));
        } catch (SAXException | IOException e) {
            // error reading the xml file bundled into the jar
            throw new IllegalStateException("Error while opening local resource " + resource, e);
        }
    }

    static class EditAndCommitResponse {
        private final Document editResponse, commitResponse;

        EditAndCommitResponse(Document editResponse, Document commitResponse) {
            this.editResponse = editResponse;
            this.commitResponse = commitResponse;
        }

        public Document getEditResponse() {
            return editResponse;
        }

        public Document getCommitResponse() {
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
}
