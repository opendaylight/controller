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
    public void testCreateInstanceWithPersistentActorSystem() throws Exception {
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

    @Test
    public void testCreateInstanceWithNonPersistentActorSystem() throws Exception {
        DatastoreContext datastoreContext = mock(DatastoreContext.class);
        when(datastoreContext.isPersistent()).thenReturn(false);
        when(datastoreContext.getConfigurationReader()).thenReturn(new ResourceConfigurationReader());

        SchemaService schemaService = mock(SchemaService.class);

        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle()).thenReturn(mock(Bundle.class));


        try (DistributedDataStore dataStore = DistributedDataStoreFactory.createInstance("test", schemaService, datastoreContext, bundleContext)) {
            assertNotNull(dataStore);
        }
    }

}