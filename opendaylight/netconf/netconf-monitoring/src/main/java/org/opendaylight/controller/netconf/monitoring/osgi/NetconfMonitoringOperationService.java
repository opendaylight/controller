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
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.monitoring.Get;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetconfMonitoringOperationService implements NetconfOperationService {

    public static final HashSet<Capability> CAPABILITIES = Sets.<Capability>newHashSet(new Capability() {

        @Override
        public String getCapabilityUri() {
            return MonitoringConstants.NAMESPACE;
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
        public Optional<List<String>> getLocation() {
            return Optional.absent();
        }
    });

    private final NetconfMonitoringService monitor;

    public NetconfMonitoringOperationService(NetconfMonitoringService monitor) {
        this.monitor = monitor;
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
