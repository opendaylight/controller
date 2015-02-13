/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring.osgi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

public class NetconfMonitoringOperationServiceTest {
    @Test
    public void testGetters() throws Exception {
        NetconfMonitoringService monitor = mock(NetconfMonitoringService.class);
        NetconfMonitoringOperationService service = new NetconfMonitoringOperationService(monitor);
        NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory serviceFactory = new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(service);

        assertEquals(2, service.getNetconfOperations().size());

        assertEquals(Optional.<String>absent(), serviceFactory.getCapabilities().iterator().next().getCapabilitySchema());
        assertEquals(Collections.<String>emptyList(), serviceFactory.getCapabilities().iterator().next().getLocation());
        assertEquals(Optional.of(MonitoringConstants.MODULE_REVISION), serviceFactory.getCapabilities().iterator().next().getRevision());
        assertEquals(Optional.of(MonitoringConstants.MODULE_NAME), serviceFactory.getCapabilities().iterator().next().getModuleName());
        assertEquals(Optional.of(MonitoringConstants.NAMESPACE), serviceFactory.getCapabilities().iterator().next().getModuleNamespace());
        assertEquals(MonitoringConstants.URI, serviceFactory.getCapabilities().iterator().next().getCapabilityUri());
    }
}
