/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Collections2;
import java.io.IOException;
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
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanServerConnection;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleFactoryNotFoundException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.facade.xml.ConfigExecution;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@Immutable
public class ConfigPusherImpl implements ConfigPusher {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigPusherImpl.class);

    private static final Date NO_REVISION = new Date(0);
    private static final int QUEUE_SIZE = 100;

    private final long maxWaitForCapabilitiesMillis;
    private final long conflictingVersionTimeoutMillis;
    private final BlockingQueue<List<? extends ConfigSnapshotHolder>> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    private final ConfigSubsystemFacadeFactory facade;
    private ConfigPersisterNotificationHandler jmxNotificationHandler;

    public ConfigPusherImpl(final ConfigSubsystemFacadeFactory facade, final long maxWaitForCapabilitiesMillis,
                        final long conflictingVersionTimeoutMillis) {
        this.maxWaitForCapabilitiesMillis = maxWaitForCapabilitiesMillis;
        this.conflictingVersionTimeoutMillis = conflictingVersionTimeoutMillis;
        this.facade = facade;
    }

    public void process(final List<AutoCloseable> autoCloseables, final MBeanServerConnection platformMBeanServer,
            final Persister persisterAggregator, final boolean propagateExceptions) throws InterruptedException {
        while(processSingle(autoCloseables, platformMBeanServer, persisterAggregator, propagateExceptions)) {
        }
    }

    boolean processSingle(final List<AutoCloseable> autoCloseables, final MBeanServerConnection platformMBeanServer,
            final Persister persisterAggregator, final boolean propagateExceptions) throws InterruptedException {
        final List<? extends ConfigSnapshotHolder> configs = queue.take();
        try {
            internalPushConfigs(configs);

            // Do not register multiple notification handlers
            if(jmxNotificationHandler == null) {
                jmxNotificationHandler =
                        new ConfigPersisterNotificationHandler(platformMBeanServer, persisterAggregator, facade);
                synchronized (autoCloseables) {
                    autoCloseables.add(jmxNotificationHandler);
                }
            }

            LOG.debug("ConfigPusher has pushed configs {}", configs);
        } catch (final Exception e) {
            // Exceptions are logged to error downstream
            LOG.debug("Failed to push some of configs: {}", configs, e);

            if(propagateExceptions) {
                if(e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else {
                    throw new IllegalStateException(e);
                }
            } else {
                return false;
            }
        }

        return true;
    }

    @Override
    public void pushConfigs(final List<? extends ConfigSnapshotHolder> configs) throws InterruptedException {
        LOG.debug("Requested to push configs {}", configs);
        this.queue.put(configs);
    }

    private LinkedHashMap<? extends ConfigSnapshotHolder, Boolean> internalPushConfigs(final List<? extends ConfigSnapshotHolder> configs)
            throws DocumentedException {
        LOG.debug("Last config snapshots to be pushed to netconf: {}", configs);
        LinkedHashMap<ConfigSnapshotHolder, Boolean> result = new LinkedHashMap<>();
        // start pushing snapshots
        for (ConfigSnapshotHolder configSnapshotHolder : configs) {
            if (configSnapshotHolder != null) {
                LOG.info("Pushing configuration snapshot {}", configSnapshotHolder);
                boolean pushResult = false;
                try {
                    pushResult = pushConfigWithConflictingVersionRetries(configSnapshotHolder);
                } catch (final ConfigSnapshotFailureException e) {
                    LOG.error("Failed to apply configuration snapshot: {}. Config snapshot is not semantically correct and will be IGNORED. " +
                            "for detailed information see enclosed exception.", e.getConfigIdForReporting(), e);
                    throw new IllegalStateException("Failed to apply configuration snapshot " + e.getConfigIdForReporting(), e);
                }  catch (final Exception e) {
                    String msg = String.format("Failed to apply configuration snapshot: %s", configSnapshotHolder);
                    LOG.error(msg, e);
                    throw new IllegalStateException(msg, e);
                }

                LOG.info("Successfully pushed configuration snapshot {}", configSnapshotHolder);
                result.put(configSnapshotHolder, pushResult);
            }
        }
        LOG.debug("All configuration snapshots have been pushed successfully.");
        return result;
    }

    private synchronized boolean pushConfigWithConflictingVersionRetries(final ConfigSnapshotHolder configSnapshotHolder) throws ConfigSnapshotFailureException {
        ConflictingVersionException lastException;
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        do {
            //TODO wait untill all expected modules are in yangStoreService, do we even need to with yangStoreService instead on netconfOperationService?
            String idForReporting = configSnapshotHolder.toString();
            SortedSet<String> expectedCapabilities = checkNotNull(configSnapshotHolder.getCapabilities(),
                    "Expected capabilities must not be null - %s, check %s", idForReporting,
                    configSnapshotHolder.getClass().getName());

            // wait max time for required capabilities to appear
            waitForCapabilities(expectedCapabilities, idForReporting);
            try {
                if(!stopwatch.isRunning()) {
                    stopwatch.start();
                }
                return pushConfig(configSnapshotHolder);
            } catch (final ConflictingVersionException e) {
                lastException = e;
                LOG.info("Conflicting version detected, will retry after timeout");
                sleep();
            }
        } while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < conflictingVersionTimeoutMillis);
        throw new IllegalStateException("Max wait for conflicting version stabilization timeout after " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms",
                lastException);
    }

    private void waitForCapabilities(final Set<String> expectedCapabilities, final String idForReporting) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ConfigPusherException lastException;
        do {
            try {
                final Set<Capability> currentCaps = facade.getCurrentCapabilities();
                final Set<String> notFoundCapabilities = computeNotFoundCapabilities(expectedCapabilities, currentCaps);
                if (notFoundCapabilities.isEmpty()) {
                    return;
                } else {
                    LOG.debug("Netconf server did not provide required capabilities for {} ", idForReporting,
                            "Expected but not found: {}, all expected {}, current {}",
                            notFoundCapabilities, expectedCapabilities, currentCaps
                    );
                    throw new NotEnoughCapabilitiesException(
                            "Not enough capabilities for " + idForReporting + ". Expected but not found: " + notFoundCapabilities, notFoundCapabilities);
                }
            } catch (final ConfigPusherException e) {
                LOG.debug("Not enough capabilities: {}", e.toString());
                lastException = e;
                sleep();
            }
        } while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < maxWaitForCapabilitiesMillis);

        LOG.error("Unable to push configuration due to missing yang models." +
                        " Yang models that are missing, but required by the configuration: {}." +
                        " For each mentioned model check: " +
                        " 1. that the mentioned yang model namespace/name/revision is identical to those in the yang model itself" +
                        " 2. the yang file is present in the system" +
                        " 3. the bundle with that yang file is present in the system and active" +
                        " 4. the yang parser did not fail while attempting to parse that model",
                ((NotEnoughCapabilitiesException) lastException).getMissingCaps());
        throw new IllegalStateException("Unable to push configuration due to missing yang models." +
                " Required yang models that are missing: "
                + ((NotEnoughCapabilitiesException) lastException).getMissingCaps(), lastException);
    }

    private static Set<String> computeNotFoundCapabilities(final Set<String> expectedCapabilities, final Set<Capability> currentCapabilities) {
        Collection<String> actual = transformCapabilities(currentCapabilities);
        Set<String> allNotFound = new HashSet<>(expectedCapabilities);
        allNotFound.removeAll(actual);
        return allNotFound;
    }

    static Set<String> transformCapabilities(final Set<Capability> currentCapabilities) {
        return new HashSet<>(Collections2.transform(currentCapabilities, Capability::getCapabilityUri));
    }

    static class ConfigPusherException extends Exception {

        public ConfigPusherException(final String message) {
            super(message);
        }

        public ConfigPusherException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    static class NotEnoughCapabilitiesException extends ConfigPusherException {
        private static final long serialVersionUID = 1L;
        private final Set<String> missingCaps;

        NotEnoughCapabilitiesException(final String message, final Set<String> missingCaps) {
            super(message);
            this.missingCaps = missingCaps;
        }

        public Set<String> getMissingCaps() {
            return missingCaps;
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

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private synchronized boolean pushConfig(final ConfigSnapshotHolder configSnapshotHolder) throws ConfigSnapshotFailureException, ConflictingVersionException {
        Element xmlToBePersisted;
        try {
            xmlToBePersisted = XmlUtil.readXmlToElement(configSnapshotHolder.getConfigSnapshot());
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Cannot parse " + configSnapshotHolder, e);
        }
        LOG.trace("Pushing last configuration to config mapping: {}", configSnapshotHolder);

        Stopwatch stopwatch = Stopwatch.createStarted();
        final ConfigSubsystemFacade currentFacade = this.facade.createFacade("config-push");
        try {
            ConfigExecution configExecution = createConfigExecution(xmlToBePersisted, currentFacade);
            executeWithMissingModuleFactoryRetries(currentFacade, configExecution);
        } catch (ValidationException | DocumentedException | ModuleFactoryNotFoundException e) {
            LOG.trace("Validation for config: {} failed", configSnapshotHolder, e);
            throw new ConfigSnapshotFailureException(configSnapshotHolder.toString(), "edit", e);
        }

        try {
            currentFacade.commitSilentTransaction();
        } catch (ValidationException | DocumentedException e) {
            throw new ConfigSnapshotFailureException(configSnapshotHolder.toString(), "commit", e);
        }

        LOG.trace("Last configuration loaded successfully");
        LOG.trace("Total time spent {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return true;
    }

    private void executeWithMissingModuleFactoryRetries(final ConfigSubsystemFacade facade, final ConfigExecution configExecution)
            throws DocumentedException, ValidationException, ModuleFactoryNotFoundException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ModuleFactoryNotFoundException lastException = null;
        do {
            try {
                facade.executeConfigExecution(configExecution);
                return;
            } catch (final ModuleFactoryNotFoundException e) {
                LOG.debug("{} - will retry after timeout", e.toString());
                lastException = e;
                sleep();
            }
        } while (stopwatch.elapsed(TimeUnit.MILLISECONDS) < maxWaitForCapabilitiesMillis);

        throw lastException;
    }

    private ConfigExecution createConfigExecution(final Element xmlToBePersisted, final ConfigSubsystemFacade currentFacade) throws DocumentedException {
        final Config configMapping = currentFacade.getConfigMapping();
        return currentFacade.getConfigExecution(configMapping, xmlToBePersisted);
    }

}
