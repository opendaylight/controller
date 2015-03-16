/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.util.capability.BasicCapability;
import org.opendaylight.controller.netconf.util.capability.YangModuleCapability;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalNetconfOperationServiceFactory implements NetconfOperationServiceFactory, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalNetconfOperationServiceFactory.class);

    private final DOMDataBroker dataBroker;
    private final CurrentSchemaContext currentSchemaContext;

    public MdsalNetconfOperationServiceFactory(final SchemaService schemaService, final DOMDataBroker domDataBroker) {
        this.currentSchemaContext = new CurrentSchemaContext(Preconditions.checkNotNull(schemaService));
        this.dataBroker = Preconditions.checkNotNull(domDataBroker);
    }

    @Override
    public MdsalNetconfOperationService createService(final String netconfSessionIdForReporting) {
        return new MdsalNetconfOperationService(currentSchemaContext, netconfSessionIdForReporting, dataBroker);
    }

    @Override
    public void close() throws Exception {
        currentSchemaContext.close();
    }

    @Override
    public Set<Capability> getCapabilities() {
        return transformCapabilities(currentSchemaContext.getCurrentContext());
    }

    static Set<Capability> transformCapabilities(final SchemaContext currentContext1) {
        final Set<Capability> capabilities = new HashSet<>();
        // [RFC6241] 8.3.  Candidate Configuration Capability
        capabilities.add(new BasicCapability("urn:ietf:params:netconf:capability:candidate:1.0"));

        final SchemaContext currentContext = currentContext1;
        final Set<Module> modules = currentContext.getModules();
        for (final Module module : modules) {
            if(currentContext.getModuleSource(module).isPresent()) {
                capabilities.add(new YangModuleCapability(module, currentContext.getModuleSource(module).get()));
            } else {
                LOG.warn("Missing source for module {}. This module will not be available from netconf server",
                        module);
            }
        }

        return capabilities;
    }

    @Override
    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        return currentSchemaContext.registerCapabilityListener(listener);
    }

}
