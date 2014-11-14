/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Hashtable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class NetconfMonitoringServiceTrackerTest {

    @Mock
    private ServiceReference<NetconfMonitoringService> reference;
    @Mock
    private BundleContext context;
    @Mock
    private ServiceRegistration<?> serviceRegistration;
    @Mock
    private Filter filter;
    @Mock
    private NetconfMonitoringService monitoringService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(serviceRegistration).when(context).registerService(any(Class.class), any(NetconfOperationServiceFactory.class), any(Hashtable.class));
        doNothing().when(serviceRegistration).unregister();
        doReturn(filter).when(context).createFilter(anyString());
        doReturn("").when(reference).toString();
        doReturn(monitoringService).when(context).getService(any(ServiceReference.class));
    }

    @Test
    public void testAddingService() throws Exception {
        NetconfMonitoringServiceTracker tracker = new NetconfMonitoringServiceTracker(context);
        tracker.addingService(reference);
        verify(context, times(1)).registerService(any(Class.class), any(NetconfOperationServiceFactory.class), any(Hashtable.class));
        tracker.removedService(reference, null);
        verify(serviceRegistration, times(1)).unregister();
    }
}
