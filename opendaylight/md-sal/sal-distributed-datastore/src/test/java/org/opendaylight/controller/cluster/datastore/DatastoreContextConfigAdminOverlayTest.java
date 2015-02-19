/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * Unit tests for DatastoreContextConfigAdminOverlay.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("unchecked")
public class DatastoreContextConfigAdminOverlayTest {

    @Mock
    private BundleContext mockBundleContext;

    @Mock
    private ServiceReference<ConfigurationAdmin> mockConfigAdminServiceRef;

    @Mock
    private ConfigurationAdmin mockConfigAdmin;

    @Mock
    private Configuration mockConfig;

    @Mock
    private DatastoreContextIntrospector mockIntrospector;

    @Mock
    private ServiceRegistration<?> configListenerServiceReg;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        doReturn(mockConfigAdminServiceRef).when(mockBundleContext).getServiceReference(ConfigurationAdmin.class);
        doReturn(mockConfigAdmin).when(mockBundleContext).getService(mockConfigAdminServiceRef);
        doReturn(configListenerServiceReg).when(mockBundleContext).registerService(
                eq(ConfigurationListener.class.getName()), any(), any(Dictionary.class));

        doReturn(mockConfig).when(mockConfigAdmin).getConfiguration(DatastoreContextConfigAdminOverlay.CONFIG_ID);

        doReturn(DatastoreContextConfigAdminOverlay.CONFIG_ID).when(mockConfig).getPid();

    }

    @Test
    public void testUpdateOnConstruction() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("property", "value");
        doReturn(properties).when(mockConfig).getProperties();

        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                mockIntrospector, mockBundleContext);

        verify(mockIntrospector).update(properties);

        verify(mockBundleContext).ungetService(mockConfigAdminServiceRef);

        overlay.close();
    }

    @Test
    public void testUpdateOnConfigurationEvent() {
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                mockIntrospector, mockBundleContext);

        reset(mockIntrospector);

        DatastoreContext context = DatastoreContext.newBuilder().build();
        doReturn(context).when(mockIntrospector).getContext();

        DatastoreContextConfigAdminOverlay.Listener mockListener =
                mock(DatastoreContextConfigAdminOverlay.Listener.class);

        overlay.setListener(mockListener);

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("property", "value");
        doReturn(properties).when(mockConfig).getProperties();

        doReturn(true).when(mockIntrospector).update(properties);

        ArgumentCaptor<ConfigurationListener> configListener =
                ArgumentCaptor.forClass(ConfigurationListener.class);
        verify(mockBundleContext).registerService(eq(ConfigurationListener.class.getName()),
                configListener.capture(), any(Dictionary.class));

        ConfigurationEvent configEvent = mock(ConfigurationEvent.class);
        doReturn(DatastoreContextConfigAdminOverlay.CONFIG_ID).when(configEvent).getPid();
        doReturn(mockConfigAdminServiceRef).when(configEvent).getReference();
        doReturn(ConfigurationEvent.CM_UPDATED).when(configEvent).getType();

        configListener.getValue().configurationEvent(configEvent);

        verify(mockIntrospector).update(properties);

        verify(mockListener).onDatastoreContextUpdated(context);

        verify(mockBundleContext, times(2)).ungetService(mockConfigAdminServiceRef);

        overlay.close();

        verify(configListenerServiceReg).unregister();
    }

    @Test
    public void testConfigurationEventWithDifferentPid() {
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                mockIntrospector, mockBundleContext);

        reset(mockIntrospector);

        ArgumentCaptor<ConfigurationListener> configListener =
                ArgumentCaptor.forClass(ConfigurationListener.class);
        verify(mockBundleContext).registerService(eq(ConfigurationListener.class.getName()),
                configListener.capture(), any(Dictionary.class));

        ConfigurationEvent configEvent = mock(ConfigurationEvent.class);
        doReturn("other-pid").when(configEvent).getPid();
        doReturn(mockConfigAdminServiceRef).when(configEvent).getReference();
        doReturn(ConfigurationEvent.CM_UPDATED).when(configEvent).getType();

        configListener.getValue().configurationEvent(configEvent);

        verify(mockIntrospector, times(0)).update(any(Dictionary.class));

        overlay.close();
    }

    @Test
    public void testConfigurationEventWithNonUpdateEventType() {
        DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                mockIntrospector, mockBundleContext);

        reset(mockIntrospector);

        ArgumentCaptor<ConfigurationListener> configListener =
                ArgumentCaptor.forClass(ConfigurationListener.class);
        verify(mockBundleContext).registerService(eq(ConfigurationListener.class.getName()),
                configListener.capture(), any(Dictionary.class));

        ConfigurationEvent configEvent = mock(ConfigurationEvent.class);
        doReturn(DatastoreContextConfigAdminOverlay.CONFIG_ID).when(configEvent).getPid();
        doReturn(mockConfigAdminServiceRef).when(configEvent).getReference();
        doReturn(ConfigurationEvent.CM_DELETED).when(configEvent).getType();

        configListener.getValue().configurationEvent(configEvent);

        verify(mockIntrospector, times(0)).update(any(Dictionary.class));

        overlay.close();
    }
}
