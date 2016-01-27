/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import java.io.IOException;
import java.util.Collections;
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

    private EntityOwnerSelectionStrategyConfig loadStrategyConfig() {
        return EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(mockBundleContext);
    }

    @Test
    public void testReadStrategies(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("entity.type.test", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy,100");

        doReturn(props).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = loadStrategyConfig();

        assertTrue(config.isStrategyConfigured("test"));

        EntityOwnerSelectionStrategy strategy = config.createStrategy("test", Collections.<String, Long>emptyMap());
        assertTrue(strategy.toString(), strategy instanceof LastCandidateSelectionStrategy);
        assertEquals(100L, strategy.getSelectionDelayInMillis());

        final EntityOwnerSelectionStrategy strategy1 = config.createStrategy("test", Collections.<String, Long>emptyMap());
        assertEquals(strategy, strategy1);

        config.clearStrategies();

        final EntityOwnerSelectionStrategy strategy2 = config.createStrategy("test", Collections.<String, Long>emptyMap());
        assertNotEquals(strategy1, strategy2);
    }

    @Test
    public void testReadStrategiesWithIOException() throws IOException {
        doThrow(IOException.class).when(mockConfigAdmin).getConfiguration(EntityOwnerSelectionStrategyConfigReader.CONFIG_ID);

        EntityOwnerSelectionStrategyConfig config = loadStrategyConfig();

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test
    public void testReadStrategiesWithNullConfiguration() throws IOException {
        doReturn(null).when(mockConfigAdmin).getConfiguration(EntityOwnerSelectionStrategyConfigReader.CONFIG_ID);

        EntityOwnerSelectionStrategyConfig config = loadStrategyConfig();

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test
    public void testReadStrategiesWithNullConfigurationProperties() throws IOException {
        doReturn(null).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = loadStrategyConfig();

        assertFalse(config.isStrategyConfigured("test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadStrategiesInvalidDelay(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("entity.type.test", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy,foo");

        doReturn(props).when(mockConfig).getProperties();

        loadStrategyConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadStrategiesInvalidClassType(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("entity.type.test", "String,100");

        doReturn(props).when(mockConfig).getProperties();

        loadStrategyConfig();
    }

    @Test
    public void testReadStrategiesMissingDelay(){
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("entity.type.test", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy,100");
        props.put("entity.type.test1", "org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.LastCandidateSelectionStrategy");

        doReturn(props).when(mockConfig).getProperties();

        EntityOwnerSelectionStrategyConfig config = loadStrategyConfig();

        assertEquals(100, config.createStrategy("test", Collections.<String, Long>emptyMap()).getSelectionDelayInMillis());
        assertEquals(0, config.createStrategy("test2", Collections.<String, Long>emptyMap()).getSelectionDelayInMillis());
    }

}