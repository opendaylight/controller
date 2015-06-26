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

import org.junit.Test;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;

public class NetconfMonitoringOperationServiceTest {
    @Test
    public void testGetters() throws Exception {
        NetconfMonitoringService monitor = mock(NetconfMonitoringService.class);
        NetconfMonitoringOperationService service = new NetconfMonitoringOperationService(monitor);
        NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory serviceFactory = new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(service);

        assertEquals(2, service.getNetconfOperations().size());

    }
}
