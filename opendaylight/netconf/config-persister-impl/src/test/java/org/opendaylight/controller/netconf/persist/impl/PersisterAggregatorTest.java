/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.api.PropertiesProvider;
import org.opendaylight.controller.config.persist.api.StorageAdapter;
import org.opendaylight.controller.config.persist.storage.file.FileStorageAdapter;
import org.opendaylight.controller.netconf.persist.impl.osgi.ConfigPersisterActivator;
import org.opendaylight.controller.netconf.persist.impl.osgi.PropertiesProviderBaseImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PersisterAggregatorTest {
    @Mock
    TestingPropertiesProvider propertiesProvider;

    class TestingPropertiesProvider extends PropertiesProviderBaseImpl {

        TestingPropertiesProvider() {
            super(null);
        }

        @Override
        public String getFullKeyForReporting(String key) {
            return "prefix." + key;
        }

        @Override
        public String getProperty(String key) {
            throw new UnsupportedOperationException("should be mocked");
        }
    }

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
        doCallRealMethod().when(propertiesProvider).getFullKeyForReporting(anyString());
    }

    @Ignore
    @Test
    public void testFromProperties() throws Exception {
        doReturn("").when(propertiesProvider).getProperty(ConfigPersisterActivator.NETCONF_CONFIG_PERSISTER);
        doReturn(MockAdapter.class.getName()).when(propertiesProvider).getProperty(
                ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX);
        doReturn("false").when(propertiesProvider).getProperty("readOnly");

        PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);
        persisterAggregator.persistConfig(null);
        persisterAggregator.loadLastConfig();
        persisterAggregator.persistConfig(null);
        persisterAggregator.loadLastConfig();

        assertEquals(2, MockAdapter.persist);
        assertEquals(2, MockAdapter.load);
        assertEquals(1, MockAdapter.props);
    }


    @Ignore
    @Test
    public void testFromProperties2() throws Exception {
        String prefix = "";
        doReturn(prefix).when(propertiesProvider).getProperty(ConfigPersisterActivator.NETCONF_CONFIG_PERSISTER);
        doReturn(FileStorageAdapter.class.getName()).when(propertiesProvider).getProperty(
                ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX);

        doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(
                propertiesProvider).getProperty("prefix.properties.fileStorage");
        doReturn("propertiesProvider").when(propertiesProvider).toString();
        doReturn(null).when(propertiesProvider).getProperty("prefix.properties.numberOfBackups");

        PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);
    }

    @Ignore
    @Test
    public void testFromProperties3() throws Exception {
        doReturn("").when(propertiesProvider).getProperty(ConfigPersisterActivator.NETCONF_CONFIG_PERSISTER);
        doReturn(FileStorageAdapter.class.getName()).when(propertiesProvider).getProperty(
                ConfigPersisterActivator.STORAGE_ADAPTER_CLASS_PROP_SUFFIX);
        doReturn("target" + File.separator + "generated-test-sources" + File.separator + "testFile").when(
                propertiesProvider).getProperty("prefix.properties.fileStorage");
        doReturn("false").when(propertiesProvider).getProperty("readOnly");
        doReturn("propertiesProvider").when(propertiesProvider).toString();
        doReturn("0").when(propertiesProvider).getProperty("prefix.properties.numberOfBackups");
        try {
            PersisterAggregator.createFromProperties(propertiesProvider);
            fail();
        } catch (RuntimeException e) {
            assertThat(
                    e.getMessage(),
                    containsString("numberOfBackups property should be either set to positive value, or ommited. Can not be set to 0."));
        }
    }

    @Test
    public void loadLastConfig() throws Exception {
        List<PersisterAggregator.PersisterWithConfiguration> persisterWithConfigurations = new ArrayList<>();
        PersisterAggregator.PersisterWithConfiguration first = new PersisterAggregator.PersisterWithConfiguration(mock(Persister.class), false);

        ConfigSnapshotHolder ignored = mock(ConfigSnapshotHolder.class);
        doReturn(Optional.of(ignored)).when(first.storage).loadLastConfig(); // should be ignored

        ConfigSnapshotHolder used = mock(ConfigSnapshotHolder.class);
        PersisterAggregator.PersisterWithConfiguration second = new PersisterAggregator.PersisterWithConfiguration(mock(Persister.class), false);
        doReturn(Optional.of(used)).when(second.storage).loadLastConfig(); // should be used

        PersisterAggregator.PersisterWithConfiguration third = new PersisterAggregator.PersisterWithConfiguration(mock(Persister.class), false);
        doReturn(Optional.absent()).when(third.storage).loadLastConfig();

        persisterWithConfigurations.add(first);
        persisterWithConfigurations.add(second);
        persisterWithConfigurations.add(third);

        PersisterAggregator persisterAggregator = new PersisterAggregator(persisterWithConfigurations);
        Optional<ConfigSnapshotHolder> configSnapshotHolderOptional = persisterAggregator.loadLastConfig();
        assertTrue(configSnapshotHolderOptional.isPresent());
        assertEquals(used, configSnapshotHolderOptional.get());
    }

    @Ignore
    @Test
    public void test() throws Exception {
//        Persister storage = mock(Persister.class);
//        doReturn(null).when(storage).loadLastConfig();
//        doNothing().when(storage).persistConfig(any(ConfigSnapshotHolder.class));
//
//        PersisterAggregator persister = new PersisterAggregator(storage);
//        persister.loadLastConfig();
//        persister.persistConfig(null);
//
//        verify(storage).loadLastConfig();
//        verify(storage).persistConfig(any(ConfigSnapshotHolder.class));
    }

    public static class MockAdapter implements StorageAdapter, Persister {

        static int persist = 0;

        @Override
        public void persistConfig(ConfigSnapshotHolder holder) throws IOException {
            persist++;
        }

        static int load = 0;

        @Override
        public Optional<ConfigSnapshotHolder> loadLastConfig() throws IOException {
            load++;
            return Optional.absent();
        }

        static int props = 0;

        @Override
        public Persister instantiate(PropertiesProvider propertiesProvider) {
            props++;
            return this;
        }

        @Override
        public void close() {
        }

    }

}
