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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.monitoring.Get;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

public class NetconfMonitoringOperationService implements NetconfOperationService {

    private static final Set<Capability> CAPABILITIES = Sets.<Capability>newHashSet(new Capability() {

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
        public Collection<String> getLocation() {
            return Collections.emptyList();
        }
    });

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
