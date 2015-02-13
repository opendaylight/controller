/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.base.Optional;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfOperationServiceFactoryImpl implements NetconfOperationServiceFactory {

    public static final int ATTEMPT_TIMEOUT_MS = 1000;
    private static final int SILENT_ATTEMPTS = 30;

    private final YangStoreService yangStoreService;
    private final ConfigRegistryJMXClient jmxClient;

    private static final Logger LOG = LoggerFactory.getLogger(NetconfOperationServiceFactoryImpl.class);

    public NetconfOperationServiceFactoryImpl(YangStoreService yangStoreService) {
        this(yangStoreService, ManagementFactory.getPlatformMBeanServer());
    }

    public NetconfOperationServiceFactoryImpl(YangStoreService yangStoreService, MBeanServer mBeanServer) {
        this.yangStoreService = yangStoreService;

        ConfigRegistryJMXClient configRegistryJMXClient;
        int i = 0;
        // Config registry might not be present yet, but will be eventually
        while(true) {

            try {
                configRegistryJMXClient = new ConfigRegistryJMXClient(mBeanServer);
                break;
            } catch (IllegalStateException e) {
                ++i;
                if (i > SILENT_ATTEMPTS) {
                    LOG.info("JMX client not created after {} attempts, still trying", i, e);
                } else {
                    LOG.debug("JMX client could not be created, reattempting, try {}", i, e);
                }
                try {
                    Thread.sleep(ATTEMPT_TIMEOUT_MS);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while reattempting connection", e1);
                }
            }
        }

        jmxClient = configRegistryJMXClient;
        if (i > SILENT_ATTEMPTS) {
            LOG.info("Created JMX client after {} attempts", i);
        } else {
            LOG.debug("Created JMX client after {} attempts", i);
        }
    }

    @Override
    public NetconfOperationServiceImpl createService(String netconfSessionIdForReporting) {
        return new NetconfOperationServiceImpl(yangStoreService, jmxClient, netconfSessionIdForReporting);
    }


    @Override
    public Set<Capability> getCapabilities() {
        return setupCapabilities(yangStoreService);
    }

    @Override
    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        return yangStoreService.registerCapabilityListener(listener);
    }

    public static Set<Capability> setupCapabilities(final YangStoreContext yangStoreSnapshot) {
        Set<Capability> capabilities = new HashSet<>();
        // [RFC6241] 8.3.  Candidate Configuration Capability
        capabilities.add(new BasicCapability("urn:ietf:params:netconf:capability:candidate:1.0"));

        // TODO rollback on error not supported EditConfigXmlParser:100
        // [RFC6241] 8.5.  Rollback-on-Error Capability
        // capabilities.add(new BasicCapability("urn:ietf:params:netconf:capability:rollback-on-error:1.0"));

        Set<Module> modules = yangStoreSnapshot.getModules();
        for (Module module : modules) {
            capabilities.add(new YangStoreCapability(module, yangStoreSnapshot.getModuleSource(module)));
        }

        return capabilities;
    }

    private static class BasicCapability implements Capability {

        private final String capability;

        private BasicCapability(final String capability) {
            this.capability = capability;
        }

        @Override
        public String getCapabilityUri() {
            return capability;
        }

        @Override
        public Optional<String> getModuleNamespace() {
            return Optional.absent();
        }

        @Override
        public Optional<String> getModuleName() {
            return Optional.absent();
        }

        @Override
        public Optional<String> getRevision() {
            return Optional.absent();
        }

        @Override
        public Optional<String> getCapabilitySchema() {
            return Optional.absent();
        }

        @Override
        public Collection<String> getLocation() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return capability;
        }
    }

    private static final class YangStoreCapability extends BasicCapability {

        private final String content;
        private final String revision;
        private final String moduleName;
        private final String moduleNamespace;

        public YangStoreCapability(final Module module, final String moduleContent) {
            super(toCapabilityURI(module));
            this.content = moduleContent;
            this.moduleName = module.getName();
            this.moduleNamespace = module.getNamespace().toString();
            this.revision = Util.writeDate(module.getRevision());
        }

        @Override
        public Optional<String> getCapabilitySchema() {
            return Optional.of(content);
        }

        private static String toCapabilityURI(final Module module) {
            return String.valueOf(module.getNamespace()) + "?module="
                    + module.getName() + "&revision=" + Util.writeDate(module.getRevision());
        }

        @Override
        public Optional<String> getModuleName() {
            return Optional.of(moduleName);
        }

        @Override
        public Optional<String> getModuleNamespace() {
            return Optional.of(moduleNamespace);
        }

        @Override
        public Optional<String> getRevision() {
            return Optional.of(revision);
        }
    }
}
