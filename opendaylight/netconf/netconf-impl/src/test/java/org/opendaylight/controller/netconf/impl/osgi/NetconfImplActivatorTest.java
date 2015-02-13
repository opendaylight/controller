/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Dictionary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class NetconfImplActivatorTest {

    @Mock
    private BundleContext bundle;
    @Mock
    private Filter filter;
    @Mock
    private ServiceReference<?> reference;
    @Mock
    private ServiceRegistration<?> registration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(filter).when(bundle).createFilter(anyString());
        doNothing().when(bundle).addServiceListener(any(ServiceListener.class), anyString());

        ServiceReference<?>[] refs = {};
        doReturn(refs).when(bundle).getServiceReferences(anyString(), anyString());
        doReturn(Arrays.asList(refs)).when(bundle).getServiceReferences(any(Class.class), anyString());
        doReturn("").when(bundle).getProperty(anyString());
        doReturn(registration).when(bundle).registerService(any(Class.class), any(AggregatedNetconfOperationServiceFactory.class), any(Dictionary.class));
        doNothing().when(registration).unregister();
        doNothing().when(bundle).removeServiceListener(any(ServiceListener.class));
    }

    @Test
    public void testStart() throws Exception {
        NetconfImplActivator activator = new NetconfImplActivator();
        activator.start(bundle);
        verify(bundle).registerService(any(Class.class), any(AggregatedNetconfOperationServiceFactory.class), any(Dictionary.class));
        activator.stop(bundle);
    }
}
