/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.util;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiRegistrationUtilTest {

    @Test
    public void testRegisterService() throws Exception {
        final BundleContext bundleContext = mock(BundleContext.class);
        ServiceRegistration<?> registration = mockServiceRegistration();
        doReturn(registration).when(bundleContext).registerService(String.class, "string", null);
        ServiceRegistration<?> registration2 = mockServiceRegistration();
        doReturn(registration2).when(bundleContext).registerService(Object.class, "string", null);

        AutoCloseable aggregatedRegister = OsgiRegistrationUtil.registerService(bundleContext, "string", String.class, Object.class);
        aggregatedRegister.close();

        InOrder inOrder = Mockito.inOrder(registration, registration2);
        inOrder.verify(registration2).unregister();
        inOrder.verify(registration).unregister();
    }

    @Test
    public void testWrap() throws Exception {
        final ServiceRegistration<?> serviceReg = mockServiceRegistration();
        OsgiRegistrationUtil.wrap(serviceReg).close();
        verify(serviceReg).unregister();

        final BundleTracker<?> tracker = mock(BundleTracker.class);
        doNothing().when(tracker).close();
        OsgiRegistrationUtil.wrap(tracker).close();
        verify(tracker).close();

        final ServiceTracker<?, ?> sTracker = mock(ServiceTracker.class);
        doNothing().when(sTracker).close();
        OsgiRegistrationUtil.wrap(sTracker).close();
        verify(sTracker).close();
    }

    private ServiceRegistration<?> mockServiceRegistration() {
        ServiceRegistration<?> mock = mock(ServiceRegistration.class);
        doNothing().when(mock).unregister();
        return mock;
    }

    @Test
    public void testAggregate() throws Exception {

    }
}