/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.osgi.framework.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class NetconfImplActivatorTest {

    @Mock
    private BundleContext bundle;
    @Mock
    private Filter filter;
    @Mock
    private ServiceReference reference;
    @Mock
    private ServiceRegistration registration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(filter).when(bundle).createFilter(anyString());
        doNothing().when(bundle).addServiceListener(any(ServiceListener.class), anyString());

        ServiceReference[] refs = new ServiceReference[0];
        doReturn(refs).when(bundle).getServiceReferences(anyString(), anyString());
        doReturn(Arrays.asList(refs)).when(bundle).getServiceReferences(any(Class.class), anyString());
        doReturn("").when(bundle).getProperty(anyString());
        doReturn(registration).when(bundle).registerService(any(Class.class), any(NetconfOperationServiceFactoryListenerImpl.class), any(Dictionary.class));
        doNothing().when(registration).unregister();
        doNothing().when(bundle).removeServiceListener(any(ServiceListener.class));
    }

    @Test
    public void testStart() throws Exception {
        NetconfImplActivator activator = new NetconfImplActivator();
        activator.start(bundle);
        verify(bundle, times(2)).registerService(any(Class.class), any(NetconfOperationServiceFactoryListenerImpl.class), any(Dictionary.class));
        activator.stop(bundle);
    }
}
