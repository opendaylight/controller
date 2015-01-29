/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.osgi;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.monitoring.Get;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.$YangModuleInfoImpl;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NetconfMonitoringOperationService implements NetconfOperationService {

    private static final Set<Capability> CAPABILITIES = Sets.<Capability>newHashSet();

    static {

        YangModuleInfo instance = $YangModuleInfoImpl.getInstance();
        ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Collections.singleton(instance));
        SchemaContext schemaContext = moduleInfoBackedContext.tryToCreateSchemaContext().get();

        for (final Module module : schemaContext.getModules()) {

            CAPABILITIES.add(new Capability() {

                @Override
                public String getCapabilityUri() {

                    return String.format("%s?module=%s&revision=%s", String.valueOf(module.getNamespace()), module.getName(), NetconfUtil.writeDate(module.getRevision()));
                }

                @Override
                public Optional<String> getModuleNamespace() {
                    return Optional.of(module.getNamespace().toString());
                }

                @Override
                public Optional<String> getModuleName() {
                    return Optional.of(module.getName());
                }

                @Override
                public Optional<String> getRevision() {
                    return Optional.of(NetconfUtil.writeDate(module.getRevision()));
                }

                @Override
                public Optional<String> getCapabilitySchema() {
                    return Optional.of(module.getSource());
                }

                @Override
                public List<String> getLocation() {
                    return Collections.emptyList();
                }

                public String toString(){

                    return String.format("CAPABILITIES{URI='%s', namespace=%s, name=%s, revision=%s, capabilitySchema=%s}",
                            getCapabilityUri(),getModuleNamespace(), getModuleName(), getRevision(), getCapabilitySchema());
                }

            });
        }
    }

    private final NetconfMonitoringService monitor;

    public NetconfMonitoringOperationService(final NetconfMonitoringService monitor) {
        this.monitor = monitor;
    }

    @Override
    public Set<Capability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        return Sets.<NetconfOperation>newHashSet(new Get(monitor));
    }

    @Override
    public void close() {
    }

}
