/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.config.api.LookupRegistry;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Manages life cycle of {@link YangStoreSnapshot}.
 */
public class NetconfOperationServiceImpl implements NetconfOperationService {

    private final YangStoreSnapshot yangStoreSnapshot;
    private final NetconfOperationProvider operationProvider;
    private final Set<Capability> capabilities;
    private final TransactionProvider transactionProvider;

    public NetconfOperationServiceImpl(final YangStoreService yangStoreService, final ConfigRegistryJMXClient jmxClient,
            final String netconfSessionIdForReporting) throws YangStoreException {

        yangStoreSnapshot = yangStoreService.getYangStoreSnapshot();
        checkConsistencyBetweenYangStoreAndConfig(jmxClient, yangStoreSnapshot);

        transactionProvider = new TransactionProvider(jmxClient, netconfSessionIdForReporting);
        operationProvider = new NetconfOperationProvider(yangStoreSnapshot, jmxClient, transactionProvider,
                netconfSessionIdForReporting);
        capabilities = setupCapabilities(yangStoreSnapshot);
    }


    @VisibleForTesting
    static void checkConsistencyBetweenYangStoreAndConfig(final LookupRegistry jmxClient, final YangStoreSnapshot yangStoreSnapshot) {
        Set<String> missingModulesFromConfig = Sets.newHashSet();

        Set<String> modulesSeenByConfig = jmxClient.getAvailableModuleFactoryQNames();
        Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap = yangStoreSnapshot.getModuleMXBeanEntryMap();

        for (Map<String, ModuleMXBeanEntry> moduleNameToMBE : moduleMXBeanEntryMap.values()) {
            for (ModuleMXBeanEntry moduleMXBeanEntry : moduleNameToMBE.values()) {
                String moduleSeenByYangStore = moduleMXBeanEntry.getYangModuleQName().toString();
                if(!modulesSeenByConfig.contains(moduleSeenByYangStore)){
                    missingModulesFromConfig.add(moduleSeenByYangStore);
                }
            }
        }

        Preconditions
                .checkState(
                        missingModulesFromConfig.isEmpty(),
                        "There are inconsistencies between configuration subsystem and yangstore in terms of discovered yang modules, yang modules missing from config subsystem but present in yangstore: %s, %sAll modules present in config: %s",
                        missingModulesFromConfig, System.lineSeparator(), modulesSeenByConfig);

    }

    @Override
    public void close() {
        yangStoreSnapshot.close();
        transactionProvider.close();
    }

    @Override
    public Set<Capability> getCapabilities() {
        return capabilities;
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        return operationProvider.getOperations();
    }

    private static Set<Capability> setupCapabilities(final YangStoreSnapshot yangStoreSnapshot) {
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
