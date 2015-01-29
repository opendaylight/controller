/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;


public class NetconfMonitoringOperationServiceTest {

    @Test
    public void testGetters() throws Exception {

        NetconfMonitoringService monitor = mock(NetconfMonitoringService.class);
        NetconfMonitoringOperationService service = new NetconfMonitoringOperationService(monitor);

        assertEquals(1, service.getNetconfOperations().size());
        assertEquals(2 + 2, service.getCapabilities().size());

        assertThat(Collections2.transform(service.getCapabilities(), new Function<Capability, String>() {
            @Override
            public String apply(Capability input) {
                return input.getModuleName().get();
            }
        }), CoreMatchers.hasItems(MonitoringConstants.MODULE_NAME, "ietf-inet-types", "ietf-yang-types", "ietf-netconf-monitoring-extension"));

    }
}
