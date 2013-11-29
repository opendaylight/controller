/*
* Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.netconf.monitoring.osgi;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.monitoring.Get;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetconfMonitoringOperationService implements NetconfOperationService {

    public static final HashSet<Capability> CAPABILITIES = Sets.<Capability>newHashSet(new Capability() {

        @Override
        public String getCapabilityUri() {
            return MonitoringConstants.URI;
        }

        @Override
        public Optional<String> getModuleNamespace() {
            return Optional.of(MonitoringConstants.NAMESPACE);
        }

        @Override
        public Optional<String> getModuleName() {
            return Optional.of(MonitoringConstants.MODULE_NAME);
        }

        @Override
        public Optional<String> getRevision() {
            return Optional.of(MonitoringConstants.MODULE_REVISION);
        }

        @Override
        public Optional<String> getCapabilitySchema() {
            return Optional.absent();
        }

        @Override
        public Optional<List<String>> getLocation() {
            return Optional.absent();
        }
    });

    private final NetconfMonitoringService monitor;

    public NetconfMonitoringOperationService(NetconfMonitoringService monitor) {
        this.monitor = monitor;
    }

    private static String readSchema() {
        String schemaLocation = "/META-INF/yang/ietf-netconf-monitoring.yang";
        URL resource = Schemas.class.getClassLoader().getResource(schemaLocation);
        Preconditions.checkNotNull(resource, "Unable to read schema content from %s", schemaLocation);
        File file = new File(resource.getFile());
        try {
            return Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load schema from " + schemaLocation, e);
        }
    }

    @Override
    public Set<Capability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public Set<NetconfOperation> getNetconfOperations() {
        return Collections.emptySet();
    }

    @Override
    public Set<NetconfOperationFilter> getFilters() {
        return Sets.<NetconfOperationFilter>newHashSet(new Get(monitor));
    }

    @Override
    public void close() {
    }

}
