package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import java.io.IOException;
import java.util.Hashtable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class EntityOwnerSelectionStrategyConfigReaderTest {
    @Mock
    private BundleContext mockBundleContext;

    @Mock
    private ServiceReference<ConfigurationAdmin> mockConfigAdminServiceRef;

    @Mock
    private ConfigurationAdmin mockConfigAdmin;

    @Mock
    private Configuration mockConfig;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        doReturn(mockConfigAdminServiceRef).when(mockBundleContext).getServiceReference(ConfigurationAdmin.class);
        doReturn(mockConfigAdmin).when(mockBundleContext).getService(mockConfigAdminServiceRef);

        doReturn(mockConfig).when(mockConfigAdmin).getConfiguration(EntityOwnerSelectionStrategyConfigReader.CONFIG_ID);



    }

    @Test
    public void testReadStrategies(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("test", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy,100");

        doReturn(props).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = new EntityOwnerSelectionStrategyConfigReader(mockBundleContext).getConfig();

        assertTrue(config.isStrategyConfigured("test"));

        EntityOwnerSelectionStrategy strategy = config.createStrategy("test");
        assertTrue(strategy instanceof LastCandidateSelectionStrategy);
        assertEquals(100L, strategy.getSelectionDelayInMillis());
    }

    @Test
    public void testReadStrategiesForNonExistentFile() throws IOException {
        doThrow(IOException.class).when(mockConfigAdmin).getConfiguration(EntityOwnerSelectionStrategyConfigReader.CONFIG_ID);

        EntityOwnerSelectionStrategyConfig config = new EntityOwnerSelectionStrategyConfigReader(mockBundleContext).getConfig();

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test
    public void testReadStrategiesInvalidDelay(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("test", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy,foo");

        doReturn(props).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = new EntityOwnerSelectionStrategyConfigReader(mockBundleContext).getConfig();

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test
    public void testReadStrategiesInvalidClassType(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("test", "String,100");

        doReturn(props).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = new EntityOwnerSelectionStrategyConfigReader(mockBundleContext).getConfig();

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test
    public void testReadStrategiesMissingDelay(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("test", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy,100");
        props.put("test1", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy");

        doReturn(props).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = new EntityOwnerSelectionStrategyConfigReader(mockBundleContext).getConfig();

        assertEquals(100, config.createStrategy("test").getSelectionDelayInMillis());
        assertEquals(0, config.createStrategy("test2").getSelectionDelayInMillis());
    }

}