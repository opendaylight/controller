/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class NetconfMonitoringActivatorTest {

    @Mock
    BundleContext context;
    @Mock
    Filter filter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(filter).when(context).createFilter(anyString());
        doNothing().when(context).addServiceListener(any(ServiceListener.class), anyString());
        ServiceReference<?>[] refs = new ServiceReference[2];
        doReturn(Arrays.asList(refs)).when(context).getServiceReferences(any(Class.class), anyString());
        doReturn(refs).when(context).getServiceReferences(anyString(), anyString());
        doNothing().when(context).removeServiceListener(any(ServiceListener.class));
    }

    @Test
    public void testNetconfMonitoringActivator() throws Exception {
        NetconfMonitoringActivator activator = new NetconfMonitoringActivator();
        activator.start(context);
        verify(context, times(1)).addServiceListener(any(ServiceListener.class), anyString());

        activator.stop(context);
        verify(context, times(1)).removeServiceListener(any(ServiceListener.class));
    }
}
