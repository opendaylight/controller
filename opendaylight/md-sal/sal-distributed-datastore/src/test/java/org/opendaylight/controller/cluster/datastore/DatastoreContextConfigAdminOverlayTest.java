/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Unit tests for DatastoreContextConfigAdminOverlay.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextConfigAdminOverlayTest {

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws IOException {
        BundleContext mockBundleContext = mock(BundleContext.class);
        ServiceReference<ConfigurationAdmin> mockServiceRef = mock(ServiceReference.class);
        ConfigurationAdmin mockConfigAdmin = mock(ConfigurationAdmin.class);
        Configuration mockConfig = mock(Configuration.class);
        DatastoreContextIntrospector mockIntrospector = mock(DatastoreContextIntrospector.class);

        doReturn(mockServiceRef).when(mockBundleContext).getServiceReference(ConfigurationAdmin.class);
        doReturn(mockConfigAdmin).when(mockBundleContext).getService(mockServiceRef);

        doReturn(mockConfig).when(mockConfigAdmin).getConfiguration(DatastoreContextConfigAdminOverlay.CONFIG_ID);

        doReturn(DatastoreContextConfigAdminOverlay.CONFIG_ID).when(mockConfig).getPid();

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("property", "value");
        doReturn(properties).when(mockConfig).getProperties();

        try(DatastoreContextConfigAdminOverlay overlay = new DatastoreContextConfigAdminOverlay(
                mockIntrospector, mockBundleContext)) {
        }

        verify(mockIntrospector).update(properties);

        verify(mockBundleContext).ungetService(mockServiceRef);
    }
}
