/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalNetconfOperationService implements NetconfOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalNetconfOperationService.class);

    private final CurrentSchemaContext schemaContext;
    private final String netconfSessionIdForReporting;
    private final OperationProvider operationProvider;

    public MdsalNetconfOperationService(final CurrentSchemaContext schemaContext, final String netconfSessionIdForReporting,
                                        final DOMDataBroker dataBroker) {
        this.schemaContext = schemaContext;
        // TODO schema contexts are different in data broker and the one we receive here ... the one received here should be updated same way as broker is
        this.netconfSessionIdForReporting = netconfSessionIdForReporting;
        this.operationProvider = new OperationProvider(netconfSessionIdForReporting, schemaContext, dataBroker);
    }

    @Override
    public void close() {

    }

    // TODO does this get called dynamically ?
    @Override
    public Set<Capability> getCapabilities() {
        final Set<Capability> capabilities = new HashSet<>();
        // [RFC6241] 8.3.  Candidate Configuration Capability
        capabilities.add(new BasicCapability("urn:ietf:params:netconf:capability:candidate:1.0"));

        final SchemaContext currentContext = schemaContext.getCurrentContext();
        final Set<Module> modules = currentContext.getModules();
        for (final Module module : modules) {
            if(currentContext.getModuleSource(module).isPresent()) {
                capabilities.add(new YangStoreCapability(module, currentContext.getModuleSource(module).get()));
            } else {
                LOG.warn("Missing source for module {}. This module will not be available from netconf server for session {}",
                        module, netconfSessionIdForReporting);
            }
        }

        return capabilities;
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        return operationProvider.getOperations();
    }

    // TODO reuse from netconf impl
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
            this.revision = SimpleDateFormatUtil.getRevisionFormat().format(module.getRevision());
        }

        @Override
        public Optional<String> getCapabilitySchema() {
            return Optional.of(content);
        }

        private static String toCapabilityURI(final Module module) {
            return String.valueOf(module.getNamespace()) + "?module="
                    + module.getName() + "&revision=" + SimpleDateFormatUtil.getRevisionFormat().format(module.getRevision());
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
