/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Collections2;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServerConnection;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.mapping.ConfigExecution;
import org.opendaylight.controller.config.persist.mapping.ConfigPersisterFacade;
import org.opendaylight.controller.config.persist.mapping.TestOption;
import org.opendaylight.controller.config.persist.mapping.mapping.config.Config;
import org.opendaylight.controller.config.persist.mapping.osgi.YangStoreService;
import org.opendaylight.controller.config.persist.mapping.strategy.EditStrategyType;
import org.opendaylight.controller.config.persist.mapping.transactions.TransactionProvider;
import org.opendaylight.controller.config.persist.mapping.util.Util;
import org.opendaylight.controller.config.util.ConfigRegistryClient;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@Immutable
public class ConfigPusherImpl implements ConfigPusher {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigPusherImpl.class);
    private static final Date NO_REVISION = new Date(0);

    private final long maxWaitForCapabilitiesMillis;
    private final long conflictingVersionTimeoutMillis;
    private final YangStoreService yangStoreService;
    private static final int QUEUE_SIZE = 100;
    private BlockingQueue<List<? extends ConfigSnapshotHolder>> queue = new LinkedBlockingQueue<List<? extends ConfigSnapshotHolder>>(QUEUE_SIZE);

    public ConfigPusherImpl(YangStoreService yangStoreService, long maxWaitForCapabilitiesMillis,
                        long conflictingVersionTimeoutMillis) {
        this.yangStoreService = yangStoreService;
        this.maxWaitForCapabilitiesMillis = maxWaitForCapabilitiesMillis;
        this.conflictingVersionTimeoutMillis = conflictingVersionTimeoutMillis;
    }

    public void process(List<AutoCloseable> autoCloseables, MBeanServerConnection platformMBeanServer, Persister persisterAggregator) throws InterruptedException, ValidationException, ConflictingVersionException {
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
            } catch (DocumentedException e) {
                LOG.error("Error pushing configs {}",configs);
                throw new IllegalStateException(e);
            }
        }
    }

    public void pushConfigs(List<? extends ConfigSnapshotHolder> configs) throws InterruptedException {
        LOG.debug("Requested to push configs {}", configs);
        this.queue.put(configs);
    }

    private LinkedHashMap<? extends ConfigSnapshotHolder, Boolean> internalPushConfigs(List<? extends ConfigSnapshotHolder> configs) throws DocumentedException, ConflictingVersionException, ValidationException {
        LOG.debug("Last config snapshots to be pushed to netconf: {}", configs);
        LinkedHashMap<ConfigSnapshotHolder, Boolean> result = new LinkedHashMap<>();
        // start pushing snapshots
        for (ConfigSnapshotHolder configSnapshotHolder : configs) {
            if (configSnapshotHolder != null) {
                boolean pushResult = false;
                try {
                    pushResult = pushConfigWithConflictingVersionRetries(configSnapshotHolder);
                } catch (ConfigSnapshotFailureException e) {
                    LOG.warn("Failed to apply configuration snapshot: {}. Config snapshot is not semantically correct and will be IGNORED. " +
                            "for detailed information see enclosed exception.", e.getConfigIdForReporting(), e);
                    throw new IllegalStateException("Failed to apply configuration snapshot " + e.getConfigIdForReporting(), e);
                }
                LOG.debug("Config snapshot pushed successfully: {}, result: {}", configSnapshotHolder, result);
                result.put(configSnapshotHolder, pushResult);
            }
        }
        LOG.debug("All configuration snapshots have been pushed successfully.");
        return result;
    }

    private synchronized boolean pushConfigWithConflictingVersionRetries(ConfigSnapshotHolder configSnapshotHolder) throws ConfigSnapshotFailureException, DocumentedException, ValidationException {
        ConflictingVersionException lastException;
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        do {
            //TODO wait untill all expected modules are in yangStoreService, do we even need to with yangStoreService instead on netconfOperationService?
            String idForReporting = configSnapshotHolder.toString();
            SortedSet<String> expectedCapabilities = checkNotNull(configSnapshotHolder.getCapabilities(),
                    "Expected capabilities must not be null - %s, check %s", idForReporting,
                    configSnapshotHolder.getClass().getName());
            try {
                if(!stopwatch.isRunning()) {
                    stopwatch.start();
                }
                return pushConfig(configSnapshotHolder);
            } catch (ConflictingVersionException e) {
                lastException = e;
                LOG.info("Conflicting version detected, will retry after timeout");
                sleep();
            }
        } while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < conflictingVersionTimeoutMillis);
        throw new IllegalStateException("Max wait for conflicting version stabilization timeout after " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms",
                lastException);
    }

    private static class ConfigPusherException extends Exception {

        public ConfigPusherException(final String message) {
            super(message);
        }

        public ConfigPusherException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    private static class NotEnoughCapabilitiesException extends ConfigPusherException {
        private static final long serialVersionUID = 1L;
        private Set<String> missingCaps;

        private NotEnoughCapabilitiesException(String message, Set<String> missingCaps) {
            super(message);
            this.missingCaps = missingCaps;
        }

        public Set<String> getMissingCaps() {
            return missingCaps;
        }
    }

    private static final class NetconfServiceNotAvailableException extends ConfigPusherException {

        public NetconfServiceNotAvailableException(final String s, final RuntimeException e) {
            super(s, e);
        }
    }

    private static final class ConfigSnapshotFailureException extends ConfigPusherException {

        private final String configIdForReporting;

        public ConfigSnapshotFailureException(final String configIdForReporting, final String operationNameForReporting, final Exception e) {
            super(String.format("Failed to apply config snapshot: %s during phase: %s", configIdForReporting, operationNameForReporting), e);
            this.configIdForReporting = configIdForReporting;
        }

        public String getConfigIdForReporting() {
            return configIdForReporting;
        }
    }

    private static Set<String> computeNotFoundCapabilities(Set<String> expectedCapabilities, YangStoreService yangStoreService) {

        Collection<String> actual = Collections2.transform(yangStoreService.getModules(), new Function<Module, String>() {
            @Nullable
            @Override
            public String apply(Module input) {
                final String withoutRevision = input.getNamespace().toString() + "?module=" + input.getName();
                return !input.getRevision().equals(NO_REVISION) ? withoutRevision + "&revision=" + Util.writeDate(input.getRevision()) : withoutRevision;
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

    private synchronized boolean pushConfig(ConfigSnapshotHolder configSnapshotHolder) throws DocumentedException, ConfigSnapshotFailureException, ValidationException, ConflictingVersionException {

        Element xmlToBePersisted;
        try {
            xmlToBePersisted = XmlUtil.readXmlToElement(configSnapshotHolder.getConfigSnapshot());
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Cannot parse " + configSnapshotHolder, e);
        }
        LOG.trace("Pushing last configuration to config mapping: {}", configSnapshotHolder);
        final ConfigRegistryClient configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory.getPlatformMBeanServer());
        final TransactionProvider transactionProvider = new TransactionProvider(configRegistryClient, "config-pusher-session");


        Stopwatch stopwatch = Stopwatch.createStarted();
        ConfigExecution configExecution = createConfigExecution(configRegistryClient, xmlToBePersisted);
        ConfigPersisterFacade facade = new ConfigPersisterFacade(configRegistryClient, yangStoreService, transactionProvider);
        try {
            facade.executeConfigExecution(configExecution);
        } catch (ValidationException e) {
            LOG.trace("Validation for config: {} failed", configSnapshotHolder, e);
            return false;
        }

        try {
            facade.commitTransaction();
        } catch (DocumentedException e) {
            throw new ConfigSnapshotFailureException("commit-transaction", "commit", e);
        }

        LOG.trace("Last configuration loaded successfully");
        LOG.trace("Total time spent {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return true;
    }

    private ConfigExecution createConfigExecution(ConfigRegistryClient configRegistryClient, Element xmlToBePersisted) throws DocumentedException {
        final Config configMapping = ConfigPersisterFacade.getConfigMapping(configRegistryClient, yangStoreService);
        return new ConfigExecution(configMapping, XmlElement.fromDomElement(xmlToBePersisted), TestOption.testThenSet, EditStrategyType.getDefaultStrategy());
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
