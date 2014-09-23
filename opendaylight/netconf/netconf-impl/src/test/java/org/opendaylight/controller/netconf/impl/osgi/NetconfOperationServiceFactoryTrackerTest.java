/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class NetconfOperationServiceFactoryTrackerTest {

    @Mock
    private Filter filter;
    @Mock
    private BundleContext context;
    @Mock
    private NetconfOperationServiceFactoryListener listener;
    @Mock
    private NetconfOperationServiceFactory factory;
    @Mock
    private ServiceReference reference;

    private NetconfOperationServiceFactoryTracker tracker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(listener).onRemoveNetconfOperationServiceFactory(any(NetconfOperationServiceFactory.class));
        doReturn(filter).when(context).createFilter(anyString());
        doReturn("").when(reference).toString();
        doReturn(factory).when(context).getService(any(ServiceReference.class));
        doReturn("").when(factory).toString();
        doNothing().when(listener).onAddNetconfOperationServiceFactory(any(NetconfOperationServiceFactory.class));
        tracker = new NetconfOperationServiceFactoryTracker(context, listener);
    }

    @Test
    public void testNetconfOperationServiceFactoryTracker() throws Exception {
        tracker.removedService(null, factory);
        verify(listener, times(1)).onRemoveNetconfOperationServiceFactory(any(NetconfOperationServiceFactory.class));
    }

    @Test
    public void testAddingService() throws Exception {
        assertNotNull(tracker.addingService(reference));
        verify(listener, times(1)).onAddNetconfOperationServiceFactory(any(NetconfOperationServiceFactory.class));
    }
}
