/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.config.ResourceConfigurationReader;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistributedDataStoreFactoryTest extends AbstractActorTest {

    @Test
    public void testCreateInstance() throws Exception {
        DatastoreContext datastoreContext = mock(DatastoreContext.class);
        when(datastoreContext.isPersistent()).thenReturn(true);
        when(datastoreContext.getConfigurationReader()).thenReturn(new ResourceConfigurationReader());

        SchemaService schemaService = mock(SchemaService.class);

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle()).thenReturn(mock(Bundle.class));


        try (DistributedDataStore dataStore = DistributedDataStoreFactory.createInstance("test", schemaService, datastoreContext, bundleContext)) {
            assertNotNull(dataStore);
        }
    }

}