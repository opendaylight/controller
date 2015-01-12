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

        assertEquals(1, service.getNetconfOperations().size());

        assertEquals(Optional.<String>absent(), service.getCapabilities().iterator().next().getCapabilitySchema());
        assertEquals(Collections.<String>emptyList(), service.getCapabilities().iterator().next().getLocation());
        assertEquals(Optional.of(MonitoringConstants.MODULE_REVISION), service.getCapabilities().iterator().next().getRevision());
        assertEquals(Optional.of(MonitoringConstants.MODULE_NAME), service.getCapabilities().iterator().next().getModuleName());
        assertEquals(Optional.of(MonitoringConstants.NAMESPACE), service.getCapabilities().iterator().next().getModuleNamespace());
        assertEquals(MonitoringConstants.URI, service.getCapabilities().iterator().next().getCapabilityUri());
    }
}
