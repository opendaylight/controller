/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.util.ConfigRegistryClient;

public class ConfigPersisterNotificationHandlerTest {

    @Mock
    private MBeanServerConnection mBeanServer;
    @Mock
    private Persister notificationListener;
    @Mock
    private ConfigSubsystemFacadeFactory facadeFactory;
    @Mock
    private ConfigSubsystemFacade facade;
    @Mock
    private ConfigRegistryClient configRegistryClient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(facade).when(facadeFactory).createFacade(anyString());

        doNothing().when(mBeanServer).addNotificationListener(any(ObjectName.class), any(NotificationListener.class),
                any(NotificationFilter.class), anyObject());
    }

    @Test
    public void testNotificationHandler() throws Exception {
        doReturn(true).when(mBeanServer).isRegistered(any(ObjectName.class));
        doThrow(Exception.class).when(mBeanServer).removeNotificationListener(any(ObjectName.class), any(NotificationListener.class));

        final ConfigPersisterNotificationHandler testedHandler = new ConfigPersisterNotificationHandler(mBeanServer, notificationListener, facadeFactory);
        verify(mBeanServer).addNotificationListener(any(ObjectName.class), any(NotificationListener.class),
                any(NotificationFilter.class), anyObject());

        testedHandler.close();
        verify(mBeanServer).removeNotificationListener(any(ObjectName.class), any(NotificationListener.class));
    }

    @Test
    public void testNotificationHandlerCloseNotRegistered() throws Exception {
        doReturn(false).when(mBeanServer).isRegistered(any(ObjectName.class));

        final ConfigPersisterNotificationHandler testedHandler = new ConfigPersisterNotificationHandler(mBeanServer, notificationListener, facadeFactory);

        testedHandler.close();
        verify(mBeanServer, times(0)).removeNotificationListener(any(ObjectName.class), any(NotificationListener.class));
    }
}
